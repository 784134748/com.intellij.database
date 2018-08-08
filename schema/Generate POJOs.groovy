import com.intellij.database.model.DasTable
import com.intellij.database.model.ObjectKind
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil
import lombok.AllArgsConstructor
import lombok.Builder
import lombok.Getter
import lombok.NoArgsConstructor
import lombok.Setter

/*
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */

packageName = "com.sample;"
typeMapping = [
        (~/(?i)int/)                      : "long",
        (~/(?i)float|double|decimal|real/): "double",
        (~/(?i)datetime|timestamp/)       : "java.sql.Timestamp",
        (~/(?i)date/)                     : "java.sql.Date",
        (~/(?i)time/)                     : "java.sql.Time",
        (~/(?i)/)                         : "String"
]

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
  SELECTION.filter { it instanceof DasTable }.each { generate(it, dir) }
}

def generate(table, dir) {
  def className = javaName(table.getName(), true)
  def fields = calcFields(table)
  new File(dir, className + ".java").withPrintWriter { out -> generate(out, className, fields) }
}

def generate(out, className, fields) {
  out.println "package $packageName"
  out.println ""
  out.println "import lombok.*;"
  out.println ""
  out.println "@Data"
  out.println "@NoArgsConstructor"
  out.println "@AllArgsConstructor"
  out.println "public class $className implements Serializable {"
  out.println ""
  fields.each() {
    if (it.commoent != ""){
      out.println " /**"
      out.println "  * ${it.comment}"
      out.println "  */"
    }
    if (it.commoent != ""){
      out.println "  * ${it.comment}"
    }
    if (it.annos != "") out.println "  ${it.annos}"
    out.println "  private ${it.type} ${it.name};"
    out.println ""
  }
  out.println ""
  fields.each() {
    out.println ""
    out.println "  public ${it.type} get${it.name.capitalize()}() {"
    out.println "    return ${it.name};"
    out.println "  }"
    out.println ""
    out.println "  public void set${it.name.capitalize()}(${it.type} ${it.name}) {"
    out.println "    this.${it.name} = ${it.name};"
    out.println "  }"
    out.println ""
  }
  out.println "}"
}

def calcFields(table) {
  DasUtil.getColumns(table).reduce([]) { fields, col ->
    def spec = Case.LOWER.apply(col.getDataType().getSpecification())
    def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
    fields += [[
                       name    : javaName(col.getName(), false),
                       type    : typeStr,
                       comment: col.getComment(),
                       annos   : ""]]
  }
}

def javaName(str, capitalize) {
  def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
          .collect { Case.LOWER.apply(it).capitalize() }
          .join("")
          .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
  capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
}

