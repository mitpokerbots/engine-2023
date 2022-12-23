'''
The infrastructure for interacting with the engine.
'''
import argparse
import socket
from .actions import FoldAction, CallAction, CheckAction, RaiseAction
from .states import GameState, TerminalState, RoundState
from .states import STARTING_STACK, BIG_BLIND, SMALL_BLIND
from .bot import Bot


class Runner():
    '''
    Interacts with the engine.
    '''

    def __init__(self, pokerbot, socketfile):
        self.pokerbot = pokerbot
        self.socketfile = socketfile

    def receive(self):
        '''
        Generator for incoming messages from the engine.
        '''
        while True:
            packet = self.socketfile.readline().strip().split(' ')
            if not packet:
                break
            yield packet

    def send(self, action):
        '''
        Encodes an action and sends it to the engine.
        '''
        if isinstance(action, FoldAction):
            code = 'F'
        elif isinstance(action, CallAction):
            code = 'C'
        elif isinstance(action, CheckAction):
            code = 'K'
        else:  # isinstance(action, RaiseAction)
            code = 'R' + str(action.amount)
        self.socketfile.write(code + '\n')
        self.socketfile.flush()

    def run(self):
        '''
        Reconstructs the game tree based on the action history received from the engine.
        '''
        game_state = GameState(0, 0., 1)
        round_state = None
        active = 0
        round_flag = True
        for packet in self.receive():
            for clause in packet:
                if clause[0] == 'T':
                    game_state = GameState(game_state.bankroll, float(clause[1:]), game_state.round_num)
                elif clause[0] == 'P':
                    active = int(clause[1:])
                elif clause[0] == 'H':
                    hands = [[], []]
                    hands[active] = clause[1:].split(',')
                    pips = [SMALL_BLIND, BIG_BLIND]
                    stacks = [STARTING_STACK - SMALL_BLIND, STARTING_STACK - BIG_BLIND]
                    round_state = RoundState(0, 0, pips, stacks, hands, [], None)
                    if round_flag:
                        self.pokerbot.handle_new_round(game_state, round_state, active)
                        round_flag = False
                elif clause[0] == 'F':
                    round_state = round_state.proceed(FoldAction())
                elif clause[0] == 'C':
                    round_state = round_state.proceed(CallAction())
                elif clause[0] == 'K':
                    round_state = round_state.proceed(CheckAction())
                elif clause[0] == 'R':
                    round_state = round_state.proceed(RaiseAction(int(clause[1:])))
                elif clause[0] == 'B':
                    round_state = RoundState(round_state.button, round_state.street, round_state.pips, round_state.stacks,
                                             round_state.hands, clause[1:].split(','), round_state.previous_state)
                elif clause[0] == 'O':
                    # backtrack
                    round_state = round_state.previous_state
                    revised_hands = list(round_state.hands)
                    revised_hands[1-active] = clause[1:].split(',')
                    # rebuild history
                    round_state = RoundState(round_state.button, round_state.street, round_state.pips, round_state.stacks,
                                             revised_hands, round_state.deck, round_state.previous_state)
                    round_state = TerminalState([0, 0], round_state)
                elif clause[0] == 'D':
                    assert isinstance(round_state, TerminalState)
                    delta = int(clause[1:])
                    deltas = [-delta, -delta]
                    deltas[active] = delta
                    round_state = TerminalState(deltas, round_state.previous_state)
                    game_state = GameState(game_state.bankroll + delta, game_state.game_clock, game_state.round_num)
                    self.pokerbot.handle_round_over(game_state, round_state, active)
                    game_state = GameState(game_state.bankroll, game_state.game_clock, game_state.round_num + 1)
                    round_flag = True
                elif clause[0] == 'Q':
                    return
            if round_flag:  # ack the engine
                self.send(CheckAction())
            else:
                assert active == round_state.button % 2
                action = self.pokerbot.get_action(game_state, round_state, active)
                self.send(action)


def parse_args():
    '''
    Parses arguments corresponding to socket connection information.
    '''
    parser = argparse.ArgumentParser(prog='python3 player.py')
    parser.add_argument('--host', type=str, default='localhost', help='Host to connect to, defaults to localhost')
    parser.add_argument('port', type=int, help='Port on host to connect to')
    return parser.parse_args()

def run_bot(pokerbot, args):
    '''
    Runs the pokerbot.
    '''
    assert isinstance(pokerbot, Bot)
    try:
        sock = socket.create_connection((args.host, args.port))
    except OSError:
        print('Could not connect to {}:{}'.format(args.host, args.port))
        return
    socketfile = sock.makefile('rw')
    runner = Runner(pokerbot, socketfile)
    runner.run()
    socketfile.close()
    sock.close()
