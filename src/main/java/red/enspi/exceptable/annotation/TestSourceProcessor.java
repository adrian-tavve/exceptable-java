/*
 * author     Adrian <adrian@enspi.red>
 * copyright  2024
 * license    GPL-3.0 (only)
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License, version 3.
 *  The right to apply the terms of later versions of the GPL is RESERVED.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with this program.
 *  If not, see <http://www.gnu.org/licenses/gpl-3.0.txt>.
 */
package red.enspi.exceptable.annotation;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes("red.enspi.exceptable.annotation.TestSource")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class TestSourceProcessor extends AbstractProcessor {

  public static String template_exceptableTest = """
    /*
     * _Exceptable_ test suite generated by red.enspi.exceptable.annotation.@TestSource
     */
    package {package};

    import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
    import static org.junit.jupiter.api.Assertions.assertEquals;
    import static org.junit.jupiter.api.Assertions.assertTrue;
    import org.junit.jupiter.params.ParameterizedTest;
    import org.junit.jupiter.params.provider.MethodSource;

    import red.enspi.exceptable.Exceptable;
    import red.enspi.exceptable.Exceptable.Signal;
    import red.enspi.exceptable.Exceptable.Signal.Context;
    import red.enspi.exceptable.ExceptableTest;

    import {package}.{exceptableClassname};
    import {package}.{exceptableTestSource};

    /** Test suite for {exceptableClassname}. */
    public class {exceptableClassname}Test extends {exceptableTestSource} implements ExceptableTest {

      @Override
      public Class<? extends Exceptable> exceptable() {
        return {exceptableClassname}.class;
      }

      @Override
      @ParameterizedTest
      @MethodSource("{sources_construct}")
      public void construct(Signal signal, Context context, Throwable cause) {
        assertDoesNotThrow(() -> {
          var actual = this.exceptable()
            .getDeclaredConstructor(Signal.class, Context.class, Throwable.class)
            .newInstance(signal, context, cause);
          this.signal_assertions(signal, actual);
          this.cause_assertions(cause, actual);
          this.context_assertions(context, actual.context());
          this.message_assertions(signal.message(context), actual.message(), signal);
        });
      }

      @Override
      @ParameterizedTest
      @MethodSource("{sources_SignalCode}")
      public void SignalCode(Signal signal, String expected) {
        var actual = signal.code();
        assertEquals(
          expected,
          actual,
          () -> String.format("Expected %s.code() to be '%s', but saw '%s'.", signal, expected, actual));
      }

      @Override
      @ParameterizedTest
      @MethodSource("{sources_SignalMessage}")
      public void SignalMessage(Signal signal, Context context, String expected) {
        // Signal.message(), Signal.code(), Signal.template(), Context.message()
        var actual = signal.message(context);
        assertEquals(
          expected,
          actual,
          () -> String.format("Expected signal.message(%s) to produce '%s', but saw '%s'", context, expected, actual));
        this.message_assertions(expected, actual, signal);
      }

      @Override
      @ParameterizedTest
      @MethodSource("{sources_SignalThrowable}")
      public void SignalThrowable(Signal signal, Context context, Throwable cause) {
        var actual = signal.throwable(context, cause);
        // Signal.throwable()
        assertTrue(
          actual instanceof Exceptable,
          () -> String.format(
            "Expected %s.throwable() to return an instance of Exceptable, but saw %s",
            signal,
            actual.getClass().getName()));
        if (actual instanceof Exceptable actualX) {
          this.signal_assertions(signal, actualX);
          this.cause_assertions(cause, actualX);
          this.context_assertions(context, actualX.context());
          this.message_assertions(signal.message(context), actualX.message(), signal);
        }
      }
    }

    """;

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    var messager = this.processingEnv.getMessager();
    for (var annotationElement : annotations) {
      for (Element element : roundEnv.getElementsAnnotatedWith(annotationElement)) {
        var annotation = element.getAnnotation(TestSource.class);
        messager.printMessage(
          Diagnostic.Kind.NOTE,
          String.format("Generating Exceptable test suite for %s...", element.getSimpleName()),
          element);
        var packageName = annotation.packageName();
        if (packageName == null) {
          packageName = this.findPackage(element);
        }
        String exceptableClassname = annotation.exceptableClass();
        var subs = new HashMap<String, String>();
        subs.put("{package}", packageName);
        subs.put("{exceptableClassname}", exceptableClassname);
        subs.put("{exceptableTestSource}", element.getSimpleName().toString());
        subs.put("{sources_construct}", "construct_source");
        subs.put("{sources_SignalCode}", "SignalCode_source");
        subs.put("{sources_SignalMessage}", "SignalMessage_source");
        subs.put("{sources_SignalThrowable}", "construct_source");
        var source = TestSourceProcessor.template_exceptableTest;
        for (var sub : subs.entrySet()) {
          source = source.replace(sub.getKey(), sub.getValue());
        }
        try (
          Writer writer = processingEnv.getFiler()
            .createSourceFile(packageName + "." + exceptableClassname + "Test")
            .openWriter()
        ) {
          writer.write(source);
        } catch (IOException e) {
          messager.printMessage(
            Diagnostic.Kind.ERROR,
            "Code generation failed: " + e.getMessage(),
            element);
          return false;
        }
      }
    }
    return true;
  }

  private String findPackage(Element element) {
    while (element.getEnclosingElement() instanceof Element enclosingElement) {
      if (enclosingElement instanceof PackageElement packageElement) {
        return packageElement.getQualifiedName().toString();
      }
      element = enclosingElement;
    }
    return "red.enspi.exceptable";
  }
}
