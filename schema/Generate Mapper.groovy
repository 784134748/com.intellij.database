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
typeMapping = [
        (~/(?i)bigint/)                   : "Long",
        (~/(?i)int/)                      : "Integer",
        (~/(?i)float|double|decimal|real/): "Double",
        (~/(?i)datetime|timestamp/)       : "java.time.LocalDateTime",
        (~/(?i)date/)                     : "java.time.LocalDate",
        (~/(?i)time/)                     : "java.time.LocalTime",
        (~/(?i)/)                         : "String"
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
    def tableComment = table.getComment()
    def fields = calcFields(table)
    def mapperDir = dir.toString() + sepa + "mapper" + sepa
    def baseMapperDir = dir.toString() + sepa + "mapper" + sepa + "base" + sepa
    def baseMapperFile = new File(baseMapperDir)
    baseMapperFile.mkdirs()
    new File(baseMapperDir, className + "BaseMapper.java").withPrintWriter { out -> baseMapper(out, className, tableComment, fields) }
    def mapperFile = new File(mapperDir, className + "Mapper.java")
    if (!mapperFile.exists()) {
        mapperFile.withPrintWriter { out -> mapper(out, className, fields) }
    }
}

def baseMapper(out, className, tableComment, fields) {
    out.println "package ${packageName}.mapper.base;"
    out.println ""
    out.println "import ${packageName}.model.${className};"
    out.println "import org.apache.ibatis.annotations.Param;"
    out.println ""
    out.println "import java.util.List;"
    out.println "import java.util.Map;"
    out.println ""
    out.println "public interface ${className}BaseMapper {"
    out.println ""
    out.println "    /**"
    out.println "     * 新增${tableComment}"
    out.println "     *"
    out.println "     * @param param"
    out.println "     * @return"
    out.println "     */"
    out.println "    Long insert(Map<String, Object> param);"
    out.println ""
    out.println "    /**"
    out.println "     * 通过主键删除"
    out.println "     *"
    out.println "     * @param id"
    out.println "     * @return"
    out.println "     */"
    out.println "    Integer deleteByPrimaryKey(@Param(\"id\") Long id);"
    out.println ""
    out.println "    /**"
    out.println "     * 更新${tableComment}"
    out.println "     *"
    out.println "     * @param param"
    out.println "     * @return"
    out.println "     */"
    out.println "    Integer update(Map<String, Object> param);"
    out.println ""
    out.println "    /**"
    out.println "     * 通过主键查询"
    out.println "     *"
    out.println "     * @param id"
    out.println "     * @return"
    out.println "     */"
    out.println "    ${className} selectByPrimaryKey(@Param(\"id\") Long id);"
    out.println ""
    out.println "    /**"
    out.println "     * 通过条件查询One"
    out.println "     *"
    out.println "     * @param param"
    out.println "     * @return"
    out.println "     */"
    out.println "    ${className} selectOneByQuery(Map<String, Object> param);"
    out.println ""
    out.println "    /**"
    out.println "     * 通过条件查询"
    out.println "     *"
    out.println "     * @param param"
    out.println "     * @return"
    out.println "     */"
    out.println "    List<${className}> selectByQuery(Map<String, Object> param);"
    out.println ""
    out.println "    /**"
    out.println "     * 通过条件获取下拉项"
    out.println "     *"
    out.println "     * @param param"
    out.println "     * @return"
    out.println "     */"
    out.println "    List<${basePackageName}.core.common.Select> getSelectByQuery(Map<String, Object> param);"
    out.println ""
    out.println "    /**"
    out.println "     * 通过条件查询条数"
    out.println "     *"
    out.println "     * @param param"
    out.println "     * @return"
    out.println "     */"
    out.println "    Integer count(Map<String, Object> param);"
    out.println ""
    out.println "}"
}

def mapper(out, className, fields) {
    out.println "package ${packageName}.mapper;"
    out.println ""
    out.println "import ${packageName}.mapper.base.${className}BaseMapper;"
    out.println "import org.apache.ibatis.annotations.Mapper;"
    out.println ""
    out.println "@Mapper"
    out.println "public interface ${className}Mapper extends ${className}BaseMapper {"
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
