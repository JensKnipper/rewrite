package org.openrewrite.java.recipes;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class ConvertLombokValueAnnotationToRecordTest
        implements RewriteTest {
    @Test
    void renameFieldRenamesFoundField() {
        rewriteRun(
                recipeSpec -> recipeSpec.recipe(new ConvertLombokValueAnnotationToRecord()),
                java(
                        """
                                package my.test;
                                       
                                import lombok.Value;
                                
                                @Value
                                class ValueToRecord {
                                   String test;
                                }
                                """,
                        """
                                package my.test;
                                            
                                record ValueToRecord(String test) {}
                                """
                )
        );
    }
}
