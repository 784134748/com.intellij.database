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
delete = 0
not_delete = 1
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


    //创建domain文件夹
    def domainDir = dir.toString() + sepa + "domain" + sepa
    def domainPath = new File(domainDir)
    domainPath.mkdirs()
    //创建vo文件夹
    def voDir = dir.toString() + sepa + "vo" + sepa
    def voPath = new File(voDir)
    voPath.mkdirs()
    //创建Param.java
    def paramFile = new File(domainDir, "Query" + className + "ListCondition.java")
    if (!paramFile.exists()) {
        paramFile.withPrintWriter { out -> domain(out, baseName, className, tableName, paramName, tableComment, fields) }
    }
    //创建VO.java
    def voFile = new File(voDir, "Query" + className + "ListDTO.java")
    if (!voFile.exists()) {
        voFile.withPrintWriter { out -> vo(out, baseName, className, tableName, paramName, tableComment, fields) }
    }
}

def domain(out, baseName, className, tableName, paramName, tableComment, fields) {
    out.println "package ${packageName}.domain;"
    out.println ""
    out.println "import lombok.*;"
    out.println ""
    out.println "/**"
    out.println " * @author "
    out.println " */"
    out.println "@Getter"
    out.println "@Setter"
    out.println "@Builder"
    out.println "public class Query${className}ListCondition {"
    out.println ""
    out.println "}"
}

def vo(out, baseName, className, tableName, paramName, tableComment, fields) {
    out.println "package ${packageName}.vo;"
    out.println ""
    out.println "import lombok.*;"
    out.println ""
    out.println "/**"
    out.println " * @author "
    out.println " */"
    out.println "@Getter"
    out.println "@Setter"
    out.println "public class Query${className}ListDTO {"
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