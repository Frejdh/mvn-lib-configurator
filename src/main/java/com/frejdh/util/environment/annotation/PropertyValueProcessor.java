package com.frejdh.util.environment.annotation;

import com.frejdh.util.common.toolbox.ReflectionUtils;
import com.frejdh.util.environment.PropertyConfigurer;
import com.google.auto.service.AutoService;
import com.sun.deploy.util.ReflectionUtil;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@AutoService(Processor.class) // Generates metadata
@SupportedAnnotationTypes({"com.frejdh.util.environment.annotation.PropertyValue"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class PropertyValueProcessor extends AbstractProcessor {

	private Messager messager; // Warning/errors upon compiling
	private List<Class<?>> supportedClasses;

	@Override
	public void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		messager = processingEnv.getMessager();
		supportedClasses = Arrays.asList(
				String.class,
				Integer.class,
				Double.class,
				Long.class,
				Float.class,
				Character.class,
				Short.class
		);
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		Set<? extends Element> rootElements = roundEnv.getRootElements();
		Set<? extends Element> annotationElements = roundEnv.getElementsAnnotatedWith(PropertyValue.class);

		for (Element element : rootElements) {
			for (Element subElement : element.getEnclosedElements().stream().filter(e -> e.getAnnotation(PropertyValue.class) != null).collect(Collectors.toList())) {
				setValue(subElement, subElement.getAnnotation(PropertyValue.class));
			}
		}

		// Returns false so that it can be passed to other annotation processors in the future
		return false;
	}

	/**
	 * // TODO: Finish it
	 * Set the value using reflection
	 *
	 * @param element The element/field to edit
	 * @param annotation The annotation which holds the values
	 */
	private void setValue(Element element, PropertyValue annotation) {
		TypeMirror fieldType = element.asType();
		String fieldClassNameFull = fieldType.toString();

		boolean isSet = false;
		for (Class<?> supportedClass : supportedClasses) {
			if (supportedClass.getName().equals(fieldClassNameFull)) {
				isSet = true;

				// Get the annotation properties
				if (annotation.refreshProperties()) // Refresh the loaded properties
					PropertyConfigurer.loadEnvironmentVariables(true);
				String annotationName = annotation.value();
				String annotationValue = PropertyConfigurer.getProperty(annotationName, annotation.defaultValue());

				try {
					Class<?> classOfField = Class.forName(fieldClassNameFull);
					if (Modifier.isStatic(classOfField.getModifiers())) { // Is static variable
						ReflectionUtils.setStaticVariable(classOfField, element.toString(), annotationValue);
					}
					else {
						// TODO: Add support for non-static stuff
						//ReflectionUtils.setVariable(fieldType.get);
					}
				} catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
					e.printStackTrace();
				}
				break;
			}
		}

		if (!isSet) {
			messager.printMessage(Diagnostic.Kind.ERROR, "@PropertyValue error:\nField was '" + element.asType()
					+ "', but only the following classes are supported: " + getSupportedClassesString()
					+ "\nError occurred at: " + getSourceOfError(element));
		}
	}

	private String getSupportedClassesString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < supportedClasses.size(); i++) {
			sb.append("'").append(supportedClasses.get(i).getCanonicalName()).append("'");
			if (i + 1 < supportedClasses.size())
				sb.append(", ");
		}

		return sb.toString();
	}

	private static String getSourceOfError(Element fieldElement) {
		return "'" + fieldElement.getEnclosingElement().toString() + "', field: '" + fieldElement + "'.";
	}
}
