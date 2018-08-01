package jb;

import static jb.RegExHelper.replaceUnlessFollowedByEscapingPackageName;
import static jb.RegExHelper.replaceUnlessPreceededBy;

import java.io.ByteArrayInputStream;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;

public class JunitConversionLogic {

    private JunitConversionLogic() {
        super();
    }

    public static String convert(String originalText) {
        String result = originalText;
        // don't update file if already on JUnit 5
        if (!originalText.contains("org.junit.jupiter")) {
            // easier to do these with plain text
            result = convertPackage(result);
            result = convertAnnotations(result);
            result = convertClassNames(result);
            result = addAssertThatImport(result);

            // easier to do move parameter order with AST parser
            CompilationUnit cu = JavaParser.parse(new ByteArrayInputStream(result.getBytes()));
            convertAssertionsAndAssumptionMethodParamOrder(cu);

            result = cu.toString();
        }

        return result;
    }

    private static String convertPackage(String originalText) {
        String result = originalText;
        result = result.replaceAll("org.junit.Assert.assertThat", "org.hamcrest.MatcherAssert.assertThat");
        result = result.replaceAll("org.junit.", "org.junit.jupiter.api.");

        return result;
    }

    private static String convertAnnotations(String originalText) {
        String result = originalText;
        result = result.replace("org.junit.jupiter.api.BeforeClass", "org.junit.jupiter.api.BeforeAll");
        result = replaceUnlessFollowedByEscapingPackageName(result, "org.junit.jupiter.api.Before", "All",
            "org.junit.jupiter.api.BeforeEach");
        result = result.replace("org.junit.jupiter.api.AfterClass", "org.junit.jupiter.api.AfterAll");
        result = replaceUnlessFollowedByEscapingPackageName(result, "org.junit.jupiter.api.After", "All",
            "org.junit.jupiter.api.AfterEach");
        result = result.replace("org.junit.jupiter.api.Ignore", "org.junit.jupiter.api.Disabled");
        result = result.replace("@BeforeClass", "@BeforeAll");
        result = replaceUnlessFollowedByEscapingPackageName(result, "@Before", "All", "@BeforeEach");
        result = result.replace("@AfterClass", "@AfterAll");
        result = replaceUnlessFollowedByEscapingPackageName(result, "@After", "All", "@AfterEach");

        result = result.replace("import static org.mockito.Matchers.any;", "import static org.mockito.Mockito.any;");

        result = result.replace("org.junit.jupiter.api.runner.RunWith", "org.junit.jupiter.api.extension.ExtendWith");
        result = result.replace("@RunWith(MockitoJUnitRunner.class)",
            "@ExtendWith(MockitoExtension.class)");

        result = result.replace("org.mockito.runners.MockitoJUnitRunner;",
            "org.mockito.junit.jupiter.MockitoExtension;");
        return result.replace("@Ignore", "@Disabled");
    }

    private static String convertClassNames(String originalText) {
        String result = originalText;
        result = result.replace("org.junit.jupiter.api.Assert", "org.junit.jupiter.api.Assertions");
        result = result.replace("org.junit.jupiter.api.Assume", "org.junit.jupiter.api.Assumptions");
        // don't update for hamcrest "MatcherAssert"
        result = replaceUnlessPreceededBy(result, "Assert.assert", "Matcher", "Assertions.assert");
        result = result.replace("Assert.fail", "Assertions.fail");
        result = result.replace("Assume.assume", "Assumptions.assume");
        return result;
    }

    // Assert that moved from junit core to hamcrest matchers
    private static String addAssertThatImport(String originalText) {
        String result = originalText;
        if (originalText.contains("assertThat") && !originalText.contains("org.hamcrest.MatcherAssert")) {
            result = result.replaceFirst("import", "import static org.hamcrest.MatcherAssert.assertThat;\nimport");
        }
        return result;
    }

    private static void convertAssertionsAndAssumptionMethodParamOrder(CompilationUnit cu) {
        new MoveMessageParameterVisitor().visit(cu, null);
    }
}
