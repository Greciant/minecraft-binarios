package com.awakenedmod.awakened.content.command;

public final class CommandIds {
   private static final String RULE_ABILITY_PREFIX = "rule_";
   public static final String TRIGGER_ALWAYS = "always";
   public static final String FAIL_NO_STAMINA = "awakened.feedback.no_stamina";
   public static final String FAIL_LOCKED = "awakened.feedback.locked";
   private static final String FAIL = "awakened.feedback.command_";
   public static final String FAIL_NO_RULE = "awakened.feedback.command_no_rule";
   public static final String FAIL_TOO_MANY_ON = "awakened.feedback.command_too_many_on";
   public static final String FAIL_MISMATCH = "awakened.feedback.command_mismatch";
   public static final String FAIL_OVER_CAP = "awakened.feedback.command_over_cap";
   public static final String FAIL_OVER_STRENGTH = "awakened.feedback.command_over_strength";
   public static final String FAIL_BAD_RULE = "awakened.feedback.command_bad_rule";
   public static final String FAIL_NO_SECONDS = "awakened.feedback.command_no_seconds";
   public static final String FAIL_NUMBER_BELOW_ONE = "awakened.feedback.command_number_below_one";
   public static final String FAIL_NO_FOREVER = "awakened.feedback.command_no_forever";
   public static final String FAIL_ALREADY_BOUND = "awakened.feedback.command_already_bound";
   private static final String SAY = "awakened.command.";
   public static final String SAY_RULE_RAN_OUT = "awakened.command.ran_out";
   public static final String SAY_BOUND_WAITING = "awakened.command.bound_waiting";
   public static final String SAY_RULE_EMPTY = "awakened.command.rule.empty";
   public static final String SAY_RULE_WITHIN = "awakened.command.rule.within";
   public static final String UNIT_LEVEL = "awakened.command.unit.level";
   public static final String UNIT_TIER = "awakened.command.unit.tier";
   public static final String UNIT_SPEED = "awakened.command.unit.speed";
   public static final String UNIT_BARE = "awakened.command.unit.bare";
   public static final String UNIT_HEALTH = "awakened.command.unit.health";
   public static final String UNIT_HEALTH_PER_SECOND = "awakened.command.unit.health_per_second";
   public static final String UNIT_BLOCKS = "awakened.command.unit.blocks";
   public static final String UNIT_MORE_DAMAGE = "awakened.command.unit.more_damage";
   public static final String SAY_RULE_FOREVER = "awakened.command.rule.forever";
   public static final String SAY_RULE_FOREVER_ONE = "awakened.command.rule.forever_one";
   public static final String SAY_RULE_FOR = "awakened.command.rule.for";

   private CommandIds() {
   }

   public static String ruleAbility(int slotIndex) {
      return "rule_" + (slotIndex + 1);
   }
}
