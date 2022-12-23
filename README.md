# MIT Pokerbots Engine
MIT Pokerbots engine and skeleton bots in Python, Java, and C++.

This is the reference implementation of the engine for playing vanilla Texas hold'em. **Do not update** this repo to implement the yearly game variant! Instead, create a new repo within this organization called mitpokerbots/engine-yyyy.

Improvements which help the engine generalize to new variants, run faster, handle bot subprocesses more safely, etc. should be incorporated into this repo.

## Dependencies
 - python>=3.5
 - cython (pip install cython)
 - eval7 (pip install eval7)
 - Java>=8 for java_skeleton
 - C++17 for cpp_skeleton
 - boost for cpp_skeleton (`sudo apt install libboost-all-dev`)
 - fmt for cpp_skeleton

## Linting
Use pylint.
