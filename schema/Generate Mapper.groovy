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
    int index_last = packageName.lastIndexOf(".")
    if (index_last != -1) {
        basePackageName = packageName.toString().substring(0, index_last)
    }
    def className = javaName(table.getName(), true)
    def paramName = javaName(table.getName(), false)
    def tableComment = table.getComment()
    def fields = calcFields(table)
    def mapperDir = dir.toString() + sepa + "mapper" + sepa
    def mapperFile = new File(mapperDir)
    mapperFile.mkdirs()
    new File(mapperDir, className + "Mapper.java").withPrintWriter { out -> mapper(out, className, paramName, tableComment, fields) }
}

def mapper(out, className, paramName, tableComment, fields) {
    out.println "package ${packageName}.mapper;"
    out.println ""
    out.println "import ${packageName}.model.${className}Model;"
    out.println "import org.apache.ibatis.annotations.Param;"
    out.println ""
    out.println "import java.util.List;"
    out.println "import java.util.Map;"
    out.println ""
    out.println "public interface ${className}Mapper {"
    out.println ""
    out.println "    /**"
    out.println "     * 新增${tableComment}"
    out.println "     *"
    out.println "     * @param ${paramName}Model"
    out.println "     * @return"
    out.println "     */"
    out.println "    Integer insert(${className}Model ${paramName}Model);"
    out.println ""
    out.println "    /**"
    out.println "     * 通过主键删除"
    out.println "     *"
    out.println "     * @param id"
    out.println "     * @return"
    out.println "     */"
    out.println "    Integer deleteByPrimaryKey(@Param(\"id\") Long id);"
    fields.each() {
        String str = it.colName
        if (str.endsWith("_id")) {
            def ForeignKey = javaName(it.colName, true)
            def foreignKey = javaName(it.colName, false)
            out.println ""
            out.println "    /**"
            out.println "     * 通过${foreignKey}删除"
            out.println "     *"
            out.println "     * @param ${foreignKey}"
            out.println "     * @return"
            out.println "     */"
            out.println "    Integer deleteBy${ForeignKey}(@Param(\"${foreignKey}\") Long ${foreignKey});"
        }
    }
    out.println ""
    out.println "    /**"
    out.println "     * 通过条件删除"
    out.println "     *"
    out.println "     * @param ${paramName}Model"
    out.println "     * @return"
    out.println "     */"
    out.println "    Integer deleteByQuery(${className}Model ${paramName}Model);"
    out.println ""
    out.println "    /**"
    out.println "     * 更新${tableComment}"
    out.println "     *"
    out.println "     * @param ${paramName}Model"
    out.println "     * @return"
    out.println "     */"
    out.println "    Integer update(${className}Model ${paramName}Model);"
    out.println ""
    out.println "    /**"
    out.println "     * 通过主键查询"
    out.println "     *"
    out.println "     * @param id"
    out.println "     * @return"
    out.println "     */"
    out.println "    ${className}Model selectByPrimaryKey(@Param(\"id\") Long id);"
    fields.each() {
        String str = it.colName
        if (str.endsWith("_id")) {
            def ForeignKey = javaName(it.colName, true)
            def foreignKey = javaName(it.colName, false)
            out.println ""
            out.println "    /**"
            out.println "     * 通过${foreignKey}查询"
            out.println "     *"
            out.println "     * @param ${foreignKey}"
            out.println "     * @return"
            out.println "     */"
            out.println "    List<${className}Model> selectBy${ForeignKey}(@Param(\"${foreignKey}\") Long ${foreignKey});"
        }
    }
    out.println ""
    out.println "    /**"
    out.println "     * 通过条件查询One"
    out.println "     *"
    out.println "     * @param ${paramName}Model"
    out.println "     * @return"
    out.println "     */"
    out.println "    ${className}Model selectOneByQuery(${className}Model ${paramName}Model);"
    out.println ""
    out.println "    /**"
    out.println "     * 通过条件查询"
    out.println "     *"
    out.println "     * @param ${paramName}Model"
    out.println "     * @return"
    out.println "     */"
    out.println "    List<${className}Model> selectByQuery(${className}Model ${paramName}Model);"
    out.println ""
    out.println "    /**"
    out.println "     * 通过条件查询条数"
    out.println "     *"
    out.println "     * @param ${paramName}Model"
    out.println "     * @return"
    out.println "     */"
    out.println "    Integer count(${className}Model ${paramName}Model);"
    out.println ""
    out.println "}"
}

boolean fieldsContainProperties(properties, fields) {
    def exist = false
    properties.each() {
        def property = it
        fields.each() {
            if (property == it.right) {
                exist = true
            }
        }
    }
    exist
}

boolean propertiesContainField(field, properties) {
    def exist = false
    properties.each() {
        if (field.right == it) {
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
