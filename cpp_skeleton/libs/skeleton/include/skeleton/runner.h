#pragma once

#include <charconv>
#include <iostream>
#include <optional>
#include <string>
#include <utility>

#include <boost/algorithm/string.hpp>
#include <boost/asio/ip/tcp.hpp>

#include <fmt/format.h>
#include <fmt/ostream.h>

#include "actions.h"
#include "constants.h"
#include "game.h"
#include "states.h"

namespace pokerbots::skeleton {

template <typename BotType> class Runner {
private:
  BotType pokerbot;
  boost::asio::ip::tcp::iostream &stream;

  template <typename Action> void send(Action const& action) {
    std::string code;
    code = fmt::format(FMT_STRING("{}"), action);
    stream << fmt::format(FMT_STRING("{}"), code) << '\n';
  }

  std::vector<std::string> receive() {
    std::string line;
    std::getline(stream, line);
    boost::algorithm::trim(line);

    std::vector<std::string> packet;
    boost::split(packet, line, boost::is_any_of(" "));
    return packet;
  }

public:
  template <typename... Args>
  Runner(boost::asio::ip::tcp::iostream &stream, Args... args)
      : pokerbot(std::forward<Args>(args)...), stream(stream) {}

  ~Runner() { stream.close(); }

  void run() {
    GameInfoPtr gameInfo = std::make_shared<GameInfo>(0, 0.0, 1);
    StatePtr roundState = std::make_shared<RoundState>(
        0, 0, std::array<int, 2>{0, 0}, std::array<int, 2>{0, 0},
        std::array<std::array<std::string, 2>, 2>{}, std::vector<std::string>{},
        nullptr);
    int active = 0;
    bool roundFlag = true;
    while (true) {
      auto packet = receive();
      for (const auto &clause : packet) {
        auto leftover = clause.substr(1);
        switch (clause[0]) {
          case 'T': {
            gameInfo = std::make_shared<GameInfo>(gameInfo->bankroll, std::stof(leftover), gameInfo->roundNum);
            break;
          }
          case 'P': {
            active = std::stoi(leftover);
            break;
          }
          case 'H': {
            std::vector<std::string> cards;
            boost::split(cards, leftover, boost::is_any_of(","));

            std::array<std::array<std::string, 2>, 2> hands;
            hands[active][0] = cards[0];
            hands[active][1] = cards[1];
            std::vector<std::string> deck;
            std::array<int, 2> pips = {SMALL_BLIND, BIG_BLIND};
            std::array<int, 2> stacks = {
                STARTING_STACK - SMALL_BLIND,
                STARTING_STACK - BIG_BLIND};
            roundState = std::make_shared<RoundState>(
                0, 0, std::move(pips), std::move(stacks), std::move(hands), std::move(deck), nullptr);
            if (roundFlag) {
              pokerbot.handleNewRound(
                  gameInfo,
                  std::static_pointer_cast<const RoundState>(roundState), active);
              roundFlag = false;
            }
            break;
          }
          case 'F': {
            roundState = std::static_pointer_cast<const RoundState>(roundState)->proceed({Action::Type::FOLD});
            break;
          }
          case 'C': {
            roundState = std::static_pointer_cast<const RoundState>(roundState)->proceed({Action::Type::CALL});
            break;
          }
          case 'K': {
            roundState = std::static_pointer_cast<const RoundState>(roundState)->proceed({Action::Type::CHECK});
            break;
          }
          case 'R': {
            roundState = std::static_pointer_cast<const RoundState>(roundState)->proceed({Action::Type::RAISE,
                                                                                          std::stoi(leftover)});
            break;
          }
          case 'B': {
            std::vector<std::string> cards;
            boost::split(cards, leftover, boost::is_any_of(","));
            auto maker = std::static_pointer_cast<const RoundState>(roundState);
            roundState = std::make_shared<RoundState>(maker->button, maker->street, maker->pips, maker->stacks,
                                                      maker->hands, cards, maker->previousState);
            break;
          }
          case 'O': {
            // backtrack
            std::vector<std::string> cards;
            boost::split(cards, leftover, boost::is_any_of(","));
            roundState = std::static_pointer_cast<const RoundState>(roundState)->previousState;
            auto maker = std::static_pointer_cast<const RoundState>(roundState);
            auto revisedHands = maker->hands;
            revisedHands[1 - active] = {cards[0], cards[1]};
            // rebuild history
            roundState = std::make_shared<RoundState>(maker->button, maker->street, maker->pips, maker->stacks,
                                                      revisedHands, maker->deck, maker->previousState);
            roundState = std::make_shared<TerminalState>(std::array<int, 2>{0, 0}, roundState);
            break;
          }
          case 'D': {
            auto delta = std::stoi(leftover);
            std::array<int, 2> deltas;
            deltas[active] = delta;
            deltas[1 - active] = -1 * delta;
            roundState = std::make_shared<TerminalState>(
                std::move(deltas),
                std::static_pointer_cast<const TerminalState>(roundState)
                    ->previousState);
            gameInfo = std::make_shared<GameInfo>(
                gameInfo->bankroll + delta, gameInfo->gameClock, gameInfo->roundNum);
            pokerbot.handleRoundOver(
                gameInfo,
                std::static_pointer_cast<const TerminalState>(roundState),
                active);
            gameInfo = std::make_shared<GameInfo>(
                gameInfo->bankroll, gameInfo->gameClock, gameInfo->roundNum + 1);
            roundFlag = true;
            break;
          }
          case 'Q': {
            return;
          }
          default: {
            break;
          }
        }
      }
      if (roundFlag) {
        send(Action {Action::Type::CHECK});
      } else {
        auto action = pokerbot.getAction(gameInfo, std::static_pointer_cast<const RoundState>(roundState), active);
        send(action);
      }
    }
  }
};

template <typename BotType, typename... Args>
void runBot(std::string &host, std::string &port, Args... args) {
  boost::asio::ip::tcp::iostream stream;
  stream.connect(host, port);
  // set TCP_NODELAY on the stream
  boost::asio::ip::tcp::no_delay option(true);
  stream.rdbuf()->set_option(option);
  if (!stream) {
    fmt::print(std::cerr, FMT_STRING("Unable to connect to {}:{}"), host, port);
    return;
  }

  auto r = Runner<BotType>(stream, std::forward<Args>(args)...);
  r.run();
}

inline std::array<std::string, 2> parseArgs(int argc, char *argv[]) {
  std::string host = "localhost";
  int port;

  bool host_flag = false;
  for (int i = 1; i < argc; i++) {
    std::string arg(argv[i]);
    if ((arg == "-h") | (arg == "--host")) {
      host_flag = true;
    } else if (arg == "--port") {
      // nothing to do
    } else if (host_flag) {
      host = arg;
      host_flag = false;
    } else {
      port = std::stoi(arg);
    }
  }

  return {host, std::to_string(port)};
}

} // namespace pokerbots::skeleton
