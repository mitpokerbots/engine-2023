#pragma once

#include <iostream>

#include "constants.h"

namespace pokerbots::skeleton {

struct Action {
  enum Type { FOLD, CALL, CHECK, RAISE };

  Type actionType;
  int amount;

  Action(Type t = Type::CHECK, int a = 0) : actionType(t), amount(a) {}

  friend std::ostream &operator<<(std::ostream &os, const Action &a);
};

} // namespace pokerbots::skeleton
