import com.intellij.database.model.DasTable
import com.intellij.database.model.ObjectKind
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

/*
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */

packageName = "com.yalonglee.learning.security"
typeMapping = [
        (~/(?i)bigint/)                   : "Long",
        (~/(?i)int/)                      : "Integer",
        (~/(?i)float|double|decimal|real/): "Double",
        (~/(?i)datetime|timestamp/)       : "java.time.LocalDateTime",
        (~/(?i)date/)                     : "java.time.LocalDate",
        (~/(?i)time/)                     : "java.time.LocalTime",
        (~/(?i)/)                         : "String"
]

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
    SELECTION.filter { it instanceof DasTable }.each { generate(it, dir) }
}

def generate(table, dir) {
    def className = javaName(table.getName(), true)
    def fields = calcFields(table)
    new File(dir, className + "Mapper.java").withPrintWriter { out -> generate(out, className, fields) }
}

def generate(out, className, fields) {
    out.println "package ${packageName}.mapper;"
    out.println ""
    out.println "import ${packageName}.model.${className};"
    out.println "import org.apache.ibatis.annotations.Mapper;"
    out.println "import org.apache.ibatis.annotations.Param;"
    out.println ""
    out.println "import java.util.List;"
    out.println "import java.util.Map;"
    out.println ""
    out.println "@Mapper"
    out.println "public interface ${className}Mapper {"
    out.println ""
    out.println "  ${className} selectByPrimaryKey(@Param(\"id\") Long id);"
    out.println ""
    out.println "  List<${className}> selectByQuery(Map<String, Object> param);"
    out.println ""
    out.println "  void deleteByPrimaryKey(@Param(\"id\") Long id);"
    out.println ""
    out.println "  Integer count(Map<String, Object> param);"
    out.println ""
    out.println "  Long insert(Map<String, Object> param);"
    out.println ""
    out.println "  void update(Map<String, Object> param);"
    out.println ""
    out.println "}"
}

def calcFields(table) {
    DasUtil.getColumns(table).reduce([]) { fields, col ->
        def spec = Case.LOWER.apply(col.getDataType().getSpecification())
        def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
        fields += [[
                           name   : javaName(col.getName(), false),
                           type   : typeStr,
                           comment: col.getComment(),
                           annos  : ""]]
    }
}

def javaName(str, capitalize) {
    def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it).capitalize() }
            .join("")
            .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
    capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
}
