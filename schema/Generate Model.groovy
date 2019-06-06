import com.intellij.database.model.DasTable
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

import java.util.regex.Matcher
import java.util.regex.Pattern

/*
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */

packageName = ""
basePackageName = ""
idProperties = ["id"] as String[]
gmtCreate = ["gmt_create"] as String[]
gmtModified = ["gmt_modified"] as String[]
isDeleteProperties = ["is_delete"] as String[]
delete = 1
commonProperties = ["id", "gmt_create", "gmt_modified"] as String[]
javaTypeMapping = [
        (~/(?i)bigint/)                   : "Long",
        (~/(?i)int|timestamp/)            : "Integer",
        (~/(?i)float|double|decimal|real/): "Double",
        (~/(?i)datetime/)                 : "java.time.LocalDateTime",
        (~/(?i)date/)                     : "java.time.LocalDate",
        (~/(?i)time/)                     : "java.time.LocalTime",
        (~/(?i)json/)                     : "String",
        (~/(?i)/)                         : "String"
]

parameterTypeMapping = [
        (~/(?i)bigint/)                   : "java.lang.Long",
        (~/(?i)int|timestamp/)            : "java.lang.Integer",
        (~/(?i)float|double|decimal|real/): "java.lang.Double",
        (~/(?i)datetime/)                 : "java.time.LocalDateTime",
        (~/(?i)date/)                     : "java.time.LocalDate",
        (~/(?i)time/)                     : "java.time.LocalTime",
        (~/(?i)json/)                     : "java.lang.String",
        (~/(?i)/)                         : "java.lang.String"
]

sepa = java.io.File.separator

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
    SELECTION.filter { it instanceof DasTable }.each { generate(it, dir) }
}

def generate(table, dir) {
    int index = dir.toString().lastIndexOf(sepa + "src" + sepa + "main" + sepa + "java" + sepa)
    if (index != -1) {
        packageName = dir.toString().substring(index + 15).replaceAll(sepa, ".")
    }
    index_last = packageName.lastIndexOf(".")
    if (index_last != -1) {
        basePackageName = packageName.toString().substring(0, index_last)
    }
    def baseName = javaName(packageName.substring(packageName.lastIndexOf(".") + 1), true)
    def className = javaName(table.getName(), true)
    def paramName = javaName(table.getName(), false)
    def fields = calcFields(table)
    def tableName = table.getName()
    def tableComment = table.getComment()


    //创建model文件夹
    def modelDir = dir.toString() + sepa + "model" + sepa
    def baseModelDir = dir.toString() + sepa + "model" + sepa + "base" + sepa
    def baseModelPath = new File(baseModelDir)
    baseModelPath.mkdirs()
    //创建BaseModel.java
    def baseModelFile = new File(baseModelDir, baseName + "BaseModel.java")
    if (!baseModelFile.exists()) {
        baseModelFile.withPrintWriter { out -> baseModel(out, baseName, className, tableName, paramName, tableComment, fields) }
    }
    //创建Model.java
    def modelFile = new File(modelDir, className + "Model.java")
//    if (!modelFile.exists()) {
    modelFile.withPrintWriter { out -> model(out, baseName, className, tableName, paramName, tableComment, fields) }
//    }
}

def baseModel(out, baseName, className, tableName, paramName, tableComment, fields) {
    out.println "package ${packageName}.model.base;"
    out.println ""
    out.println "public class ${baseName}BaseModel {"
    out.println "}"
}

def model(out, baseName, className, tableName, paramName, tableComment, fields) {
    out.println "package ${packageName}.model;"
    out.println ""
    out.println "import ${packageName}.model.base.BaseModel;"
    out.println "import com.fasterxml.jackson.annotation.JsonFormat;"
    out.println "import io.swagger.annotations.ApiModel;"
    out.println "import io.swagger.annotations.ApiModelProperty;"
    out.println "import org.springframework.format.annotation.DateTimeFormat;"
    out.println "import lombok.*;"
    out.println ""
    out.println "import java.io.Serializable;"
    out.println ""
    out.println "/**"
    out.println " * @author "
    out.println " */"
    out.println "@Data"
    out.println "@Builder"
    out.println "@NoArgsConstructor"
    out.println "@AllArgsConstructor"
    out.println "@ApiModel(value = \"${className}Model\", description = \"${tableComment}\")"
    out.println "public class ${className}Model extends ${baseName}BaseModel implements Serializable {"
    out.println ""
    out.println "    public static final long serialVersionUID = 1L;"
    out.println ""
    fields.each() {
        if (propertiesContainField(it, commonProperties)) {
            if (it.commoent != "") {
                out.println "    @ApiModelProperty(value = \"${it.comment}\", dataType = \"${it.javaType}\", hidden = true)"
            }
            if (it.annos != "") {
                out.println "    ${it.annos}"
            }
            if (it.javaType.contains("java.time.")) {
                out.println "    @DateTimeFormat(pattern = \"yyyy-MM-dd HH:mm:ss\")"
                out.println "    @JsonFormat(pattern = \"yyyy-MM-dd HH:mm:ss\")"
            }
            out.println "    private ${it.javaType} ${it.javaName};"
            out.println ""
        } else {
            if (it.commoent != "") {
                out.println "    @ApiModelProperty(value = \"${it.comment}\", dataType = \"${it.javaType}\")"
            }
            if (it.annos != "") {
                out.println "    ${it.annos}"
            }
            if (it.javaType.contains("java.time.")) {
                out.println "    @DateTimeFormat(pattern = \"yyyy-MM-dd HH:mm:ss\")"
                out.println "    @JsonFormat(pattern = \"yyyy-MM-dd HH:mm:ss\")"
            }
            out.println "    private ${it.javaType} ${it.javaName};"
            out.println ""
        }
    }
    out.println ""
    out.println "}"
}

boolean fieldsContainProperties(properties, fields) {
    def exist = false
    properties.each() {
        def property = it
        fields.each() {
            if (property == it.colName) {
                exist = true
            }
        }
    }
    exist
}

boolean propertiesContainField(field, properties) {
    def exist = false
    properties.each() {
        if (field.colName == it) {
            exist = true
        }
    }
    exist
}

def calcFields(table) {
    DasUtil.getColumns(table).reduce([]) { fields, col ->
        def spec = Case.LOWER.apply(col.getDataType().getSpecification())
        def javaTypeStr = javaTypeMapping.find { p, t -> p.matcher(spec).find() }.value
        def parameterTypeStr = parameterTypeMapping.find { p, t -> p.matcher(spec).find() }.value
        fields += [[
                           javaName     : javaName(col.getName(), false),
                           colName      : col.getName(),
                           parameterName: parameterName(col.getName(), true),
                           jdbcType     : jdbcType(col.getDataType()),
                           javaType     : javaTypeStr,
                           parameterType: parameterTypeStr,
                           comment      : col.getComment(),
                           annos        : ""]]
    }
}

def jdbcType(dataType) {
    dataTypeTmp = dataType.toString()
    Pattern pattern = Pattern.compile("([a-z]{1,20})")
    Matcher matcher = pattern.matcher(dataTypeTmp)
    while (matcher.find()) {
        result = matcher.group(1)
        return result.toUpperCase()
    }
}

def javaName(str, capitalize) {
    String strTmp = str
    if (strTmp.startsWith("is_")) {
        str = str.replaceFirst("is_", "")
    }
    def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it).capitalize() }
            .join("")
            .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
    capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
}

def parameterName(str, capitalize) {
    String strTmp = str
    if (strTmp.startsWith("is_")) {
        str = str.replaceFirst("is_", "")
    }
    def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it).capitalize() }
            .join("")
            .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
    capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
}

def tableName(str, capitalize) {
    def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it).capitalize() }
            .join("")
            .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
    capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
}



