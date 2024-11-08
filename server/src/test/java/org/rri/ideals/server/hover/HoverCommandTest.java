package org.rri.ideals.server.hover;

import com.intellij.ide.highlighter.JavaFileType;
import org.junit.jupiter.api.Test;
import org.rri.ideals.server.LspLightBasePlatformTestCase5;
import org.rri.ideals.server.TestUtil;
import org.rri.ideals.server.commands.ExecutorContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HoverCommandTest extends LspLightBasePlatformTestCase5 {

    @Test
    void testFindMethodInfo() {
        final var file = myFixture.configureByText(JavaFileType.INSTANCE, """
                public class Dummy {
                        /**
                         * A method
                         */
                        String dummyMethod(final String name) {
                          return name;
                        }
                        
                        void anotherDummyMethod() {
                           dummyMethod<caret>("foo");
                        }
                """);

        final var executorContext = new ExecutorContext(file, myFixture.getEditor(), new TestUtil.DumbCancelChecker());
        assertEquals("""
                 [`Dummy`](psi_element://Dummy)
                       
                String dummyMethod( \s
                 String name \s
                )
                
                A method""", new HoverCommand().execute(executorContext).getContents().getRight().getValue());
    }
}
