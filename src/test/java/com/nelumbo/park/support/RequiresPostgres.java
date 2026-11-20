package com.nelumbo.park.support;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Runs the test class only when {@link TestPostgres} can provide a database; otherwise it is skipped. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(RequiresPostgres.Condition.class)
public @interface RequiresPostgres {

    class Condition implements ExecutionCondition {
        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
            return TestPostgres.connection()
                    .map(c -> ConditionEvaluationResult.enabled("PostgreSQL from " + c.source()))
                    .orElse(ConditionEvaluationResult.disabled(
                            "No PostgreSQL: set TEST_POSTGRES_URL or start Docker to run this test"));
        }
    }
}
