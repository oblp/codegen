package com.github.oblp.codegen;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.codegen.*;
import org.openapitools.codegen.utils.ModelUtils;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class OblpSpringCodegen extends DefaultCodegen implements CodegenConfig {
	// source root
	protected String sourceRoot = "src/main";
	protected String apiVersion = "0.0.1";

	protected String groupId = "com.github.oblp";
	protected String artifactId = "oblp-codegen";
	protected String artifactVersion = "0.0.1";


	public OblpSpringCodegen() {
		super();

		// set the output folder here
		outputFolder = "generated-code/oblp-spring";

		modelTemplateFiles.put("model.mustache", ".java");
		apiTemplateFiles.put("apiController.mustache", "Controller.java");
		apiTemplateFiles.put("api.mustache", ".java");
		templateDir = "oblp-spring";

		/**
		 * Api Package.  Optional, if needed, this can be used in templates
		 */
		apiPackage = "org.openapitools.api";

		/**
		 * Model Package.  Optional, if needed, this can be used in templates
		 */
		modelPackage = "org.openapitools.model";

		/**
		 * Reserved words.  Override this with reserved words specific to your language
		 */
		reservedWords = new HashSet<String>(
			Arrays.asList(
				"abstract", "continue", "for", "new", "switch", "assert",
				"default", "if", "package", "synchronized", "boolean", "do", "goto", "private",
				"this", "break", "double", "implements", "protected", "throw", "byte", "else",
				"import", "public", "throws", "case", "enum", "instanceof", "return", "transient",
				"catch", "extends", "int", "short", "try", "char", "final", "interface", "static",
				"void", "class", "finally", "long", "strictfp", "volatile", "const", "float",
				"native", "super", "while", "null")
		);

		/**
		 * Additional Properties.  These values can be passed to the templates and
		 * are available in models, apis, and supporting files
		 */
		additionalProperties.put("apiVersion", apiVersion);

		/**
		 * Supporting Files.  You can write single files for the generator with the
		 * entire object tree available.  If the input file has a suffix of `.mustache
		 * it will be processed by the template engine.  Otherwise, it will be copied
		 */
		supportingFiles.add(new SupportingFile("build.mustache",   // the input template or file
			"",                                                       // the destination folder, relative `outputFolder`
			"build.gradle")                                          // the output file
		);

		/**
		 * Language Specific Primitives.  These types will not trigger imports by
		 * the client generator
		 */
		languageSpecificPrimitives = new HashSet<String>(
			Arrays.asList(
				"Type1",      // replace these with your types
				"Type2")
		);
	}

	/**
	 * Configures the type of generator.
	 *
	 * @return the CodegenType for this generator
	 * @see org.openapitools.codegen.CodegenType
	 */
	public CodegenType getTag() {
		return CodegenType.SERVER;
	}

	/**
	 * Configures a friendly name for the generator.  This will be used by the generator
	 * to select the library with the -g flag.
	 *
	 * @return the friendly name for the generator
	 */
	public String getName() {
		return "oblp-spring";
	}

	@Override
	public String getTypeDeclaration(Schema p) {
		Schema<?> schema = ModelUtils.unaliasSchema(this.openAPI, p, importMapping);
		Schema<?> target = ModelUtils.isGenerateAliasAsModel() ? p : schema;
		if (ModelUtils.isArraySchema(target)) {
			Schema<?> items = getSchemaItems((ArraySchema) schema);
			return getSchemaType(target) + "<" + getTypeDeclaration(items) + ">";
		} else if (ModelUtils.isMapSchema(target)) {
			// Note: ModelUtils.isMapSchema(p) returns true when p is a composed schema that also defines
			// additionalproperties: true
			Schema<?> inner = getAdditionalProperties(target);
			if (inner == null) {
				inner = new StringSchema().description("TODO default missing map inner type to string");
				p.setAdditionalProperties(inner);
			}
			return getSchemaType(target) + "<String, " + getTypeDeclaration(inner) + ">";
		}
		return super.getTypeDeclaration(target);

	}

	@Override
	public String getSchemaType(Schema p) {
		String openAPIType = super.getSchemaType(p);

		// don't apply renaming on types from the typeMapping
		if (typeMapping.containsKey(openAPIType)) {
			return typeMapping.get(openAPIType);
		}

		if (null == openAPIType) {
			log.error("No Type defined for Schema " + p);
		}
		return toModelName(openAPIType);
	}

	private interface DataTypeAssigner {
		void setReturnType(String returnType);

		void setReturnContainer(String returnContainer);
	}

	public String getHelp() {
		return "Generates a oblp-spring client library.";
	}

	/**
	 * Escapes a reserved word as defined in the `reservedWords` array. Handle escaping
	 * those terms here.  This logic is only called if a variable matches the reserved words
	 *
	 * @return the escaped term
	 */
	@Override
	public String escapeReservedWord(String name) {
		return "_" + name;  // add an underscore to the name
	}

	/**
	 * Location to write model files.  You can use the modelPackage() as defined when the class is
	 * instantiated
	 */
	public String modelFileFolder() {
		return outputFolder + "/" + sourceRoot + "/java/" + modelPackage().replace('.', File.separatorChar);
	}

	/**
	 * Location to write api files.  You can use the apiPackage() as defined when the class is
	 * instantiated
	 */
	@Override
	public String apiFileFolder() {
		return outputFolder + "/" + sourceRoot + "/java/" + apiPackage().replace('.', File.separatorChar);
	}

	/**
	 * override with any special text escaping logic to handle unsafe
	 * characters so as to avoid code injection
	 *
	 * @param input String to be cleaned up
	 * @return string with unsafe characters removed or escaped
	 */
	@Override
	public String escapeUnsafeCharacters(String input) {
		//TODO: check that this logic is safe to escape unsafe characters to avoid code injection
		return input;
	}

	/**
	 * Escape single and/or double quote to avoid code injection
	 *
	 * @param input String to be cleaned up
	 * @return string with quotation mark removed or escaped
	 */
	public String escapeQuotationMark(String input) {
		//TODO: check that this logic is safe to escape quotation mark to avoid code injection
		return input.replace("\"", "\\\"");
	}
}
