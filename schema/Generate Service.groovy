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

packageName = ""
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
    int index = dir.toString().lastIndexOf("/src/main/java/")
    if (index != -1) {
        packageName = dir.toString().substring(index + 15).replaceAll("/", ".")
    }
    def className = javaName(table.getName(), true)
    def paramName = javaName(table.getName(), false)
    def fields = calcFields(table)
    def serviceDir = dir.toString() + "/service/"
    def serviceImplDir = dir.toString() + "/service/impl/"
    def serviceImplFile = new File(serviceImplDir)
    serviceImplFile.mkdir()
    new File(serviceImplDir, className + "ServiceImpl.java").withPrintWriter { out -> serviceImpl(out, className, paramName, fields) }
    new File(serviceDir, className + "Service.java").withPrintWriter { out -> service(out, className, paramName, fields) }
}

def serviceImpl(out, className, paramName, fields) {
    out.println "package ${packageName}.service.impl;"
    out.println ""
    out.println "import ${packageName}.mapper.${className}Mapper;"
    out.println "import ${packageName}.model.${className};"
    out.println "import ${packageName}.service.${className}Service;"
    out.println "import org.springframework.beans.factory.annotation.Autowired;"
    out.println "import org.springframework.cglib.beans.BeanMap;"
    out.println "import org.springframework.stereotype.Service;"
    out.println ""
    out.println "import java.util.List;"
    out.println "import java.util.Map;"
    out.println ""
    out.println "@Service"
    out.println "public class ${className}ServiceImpl implements ${className}Service {"
    out.println ""
    out.println "    @Autowired"
    out.println "    @SuppressWarnings(\"all\")"
    out.println "    private ${className}Mapper ${paramName}Mapper;"
    out.println ""
    out.println "    public Map<String, Object> getParams(Object o) {"
    out.println "        return BeanMap.create(o);"
    out.println "    }"
    out.println ""
    out.println "    @Override"
    out.println "    public ${className} selectByPrimaryKey(Long id) {"
    out.println "        if (id == null || id < 1L) {"
    out.println "            return null;"
    out.println "        }"
    out.println "        return ${paramName}Mapper.selectByPrimaryKey(id);"
    out.println "    }"
    out.println ""
    out.println "    @Override"
    out.println "    public List<${className}> selectByQuery(${className} ${paramName}) {"
    out.println "        if (${paramName} == null) {"
    out.println "            return null;"
    out.println "        }"
    out.println "        return ${paramName}Mapper.selectByQuery(getParams(${paramName}));"
    out.println "    }"
    out.println ""
    out.println "    @Override"
    out.println "    public void deleteByPrimaryKey(Long id) {"
    out.println "        if (id == null || id < 1L) {"
    out.println "            return;"
    out.println "        }"
    out.println "        ${paramName}Mapper.deleteByPrimaryKey(id);"
    out.println "    }"
    out.println ""
    out.println "    @Override"
    out.println "    public Integer count(${className} ${paramName}) {"
    out.println "        if (${paramName} == null) {"
    out.println "            return -1;"
    out.println "        }"
    out.println "        return ${paramName}Mapper.count(getParams(${paramName}));"
    out.println "    }"
    out.println ""
    out.println "    @Override"
    out.println "    public Long insert(${className} ${paramName}) {"
    out.println "        if (${paramName} == null) {"
    out.println "            return -1L;"
    out.println "        }"
    out.println "        return ${paramName}Mapper.insert(getParams(${paramName}));"
    out.println "    }"
    out.println ""
    out.println "    @Override"
    out.println "    public void update(${className} ${paramName}) {"
    out.println "        if (${paramName} == null) {"
    out.println "            return;"
    out.println "        }"
    out.println "        ${paramName}Mapper.update(getParams(${paramName}));"
    out.println "    }"
    out.println ""
    out.println "}"
}

def service(out, className, paramName, fields) {
    out.println "package ${packageName}.service;"
    out.println ""
    out.println "import ${packageName}.model.${className};"
    out.println ""
    out.println "import java.util.List;"
    out.println ""
    out.println "public interface ${className}Service {"
    out.println ""
    out.println "    ${className} selectByPrimaryKey(Long id);"
    out.println ""
    out.println "    List<${className}> selectByQuery(${className} ${paramName});"
    out.println ""
    out.println "    void deleteByPrimaryKey(Long id);"
    out.println ""
    out.println "    Integer count(${className} ${paramName});"
    out.println ""
    out.println "    Long insert(${className} ${paramName});"
    out.println ""
    out.println "    void update(${className} ${paramName});"
    out.println ""
    out.println "}"
}

def calcFields(table) {
    DasUtil.getColumns(table).reduce([]) { fields, col ->
        def spec = Case.LOWER.apply(col.getDataType().getSpecification())
        def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
        fields += [[
                           left    : javaName(col.getName(), false),
                           right   : col.getName(),
                           name    : javaName(col.getName(), false),
                           dataType: col.getDataType(),
                           type    : typeStr,
                           comment : col.getComment(),
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
