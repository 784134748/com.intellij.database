import com.intellij.database.model.DasTable
import com.intellij.database.model.ObjectKind
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

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
        (~/(?i)int/)                      : "Integer",
        (~/(?i)float|double|decimal|real/): "Double",
        (~/(?i)datetime|timestamp/)       : "java.time.LocalDateTime",
        (~/(?i)date/)                     : "java.time.LocalDate",
        (~/(?i)time/)                     : "java.time.LocalTime",
        (~/(?i)/)                         : "String"
]

parameterTypeMapping = [
        (~/(?i)bigint/)                   : "java.lang.Long",
        (~/(?i)int/)                      : "java.lang.Integer",
        (~/(?i)float|double|decimal|real/): "java.lang.Double",
        (~/(?i)datetime|timestamp/)       : "java.time.LocalDateTime",
        (~/(?i)date/)                     : "java.time.LocalDate",
        (~/(?i)time/)                     : "java.time.LocalTime",
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
    def className = javaName(table.getName(), true)
    def paramName = javaName(table.getName(), false)
    def tableComment = table.getComment()
    def fields = calcFields(table)



    //创建model文件夹
    def modelDir = dir.toString() + sepa + "model" + sepa
    def modelFile = new File(modelDir)
    modelFile.mkdirs()
    //创建model文件
    new File(modelDir, className + "Model.java").withPrintWriter { out -> model(out, className, paramName, tableComment, fields) }
}

def model(out, className, paramName, tableComment, fields) {
    out.println "package ${packageName}.model;"
    out.println ""
    out.println "import com.fasterxml.jackson.annotation.JsonFormat;"
    out.println "import io.swagger.annotations.ApiModel;"
    out.println "import io.swagger.annotations.ApiModelProperty;"
    out.println "import org.springframework.format.annotation.DateTimeFormat;"
    out.println "import lombok.*;"
    out.println ""
    out.println "import java.io.Serializable;"
    out.println ""
    out.println "@Data"
    out.println "@Builder"
    out.println "@NoArgsConstructor"
    out.println "@AllArgsConstructor"
    out.println "@ApiModel(value = \"${className}Model\", description = \"${tableComment}\")"
    out.println "public class ${className}Model implements Serializable {"
    out.println ""
    out.println "    public static final long serialVersionUID = 1L;"
    out.println ""
    fields.each() {
        if (propertiesContainField(it, commonProperties)) {
            if (it.commoent != "") {
                out.println "    /**"
                out.println "     * ${it.comment}【${it.jdbcType}】"
                out.println "     */"
            }
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
                out.println "    /**"
                out.println "     * ${it.comment}【${it.jdbcType}】"
                out.println "     */"
            }
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
                           dataType     : col.getDataType(),
                           jdbcType     : col.getDataType(),
                           javaType     : javaTypeStr,
                           parameterType: parameterTypeStr,
                           comment      : col.getComment(),
                           annos        : ""]]
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



