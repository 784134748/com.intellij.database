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
        (~/(?i)float|double|real/)        : "java.lang.Double",
        (~/(?i)decimal/)                  : "java.math.BigDecimal",
        (~/(?i)datetime/)                 : "java.time.LocalDateTime",
        (~/(?i)date/)                     : "java.time.LocalDate",
        (~/(?i)time/)                     : "java.time.LocalTime",
        (~/(?i)json/)                     : "String",
        (~/(?i)/)                         : "String"
]

parameterTypeMapping = [
        (~/(?i)bigint/)                   : "java.lang.Long",
        (~/(?i)int|timestamp/)            : "java.lang.Integer",
        (~/(?i)float|double|real/)        : "java.lang.Double",
        (~/(?i)decimal/)                  : "java.math.BigDecimal",
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
    int index_last = packageName.lastIndexOf(".")
    if (index_last != -1) {
        basePackageName = packageName.toString().substring(0, index_last)
    }
    def baseName = javaName(packageName.substring(packageName.lastIndexOf(".") + 1), true)
    def className = javaName(table.getName(), true)
    def paramName = javaName(table.getName(), false)
    def fields = calcFields(table)
    def tableName = table.getName()
    def tableComment = table.getComment()


    //创建mapper文件夹
    def mapperDir = dir.toString() + sepa + "mapper" + sepa
    def baseMapperDir = dir.toString() + sepa + "mapper" + sepa + "base" + sepa
    def baseMapperPath = new File(baseMapperDir)
    baseMapperPath.mkdirs()
    //创建BaseMapper.java
    def baseMapperFile = new File(baseMapperDir, baseName + "BaseMapper.java")
    if (!baseMapperFile.exists()) {
        baseMapperFile.withPrintWriter { out -> baseMapper(out, baseName, className, tableName, paramName, tableComment, fields) }
    }
    //创建Mapper.java
    def mapperFile = new File(mapperDir, className + "Mapper.java")
    if (!mapperFile.exists()) {
        mapperFile.withPrintWriter { out -> mapper(out, baseName, className, tableName, paramName, tableComment, fields) }
    }
}

def baseMapper(out, baseName, className, tableName, paramName, tableComment, fields) {
    out.println "package ${packageName}.mapper.base;"
    out.println ""
    out.println "import org.apache.ibatis.annotations.Param;"
    out.println ""
    out.println "import java.util.List;"
    out.println ""
    out.println "/**"
    out.println " * @author "
    out.println " */"
    out.println "public interface ${baseName}BaseMapper<T> {"
    out.println ""
    out.println "    /**"
    out.println "     * 新增"
    out.println "     *"
    out.println "     * @param t"
    out.println "     * @return"
    out.println "     */"
    out.println "    Integer insert(T t);"
    out.println ""
    out.println "    /**"
    out.println "     * 通过主键删除"
    out.println "     *"
    out.println "     * @param id"
    out.println "     * @return"
    out.println "     */"
    out.println "    Integer deleteByPrimaryKey(@Param(\"id\") Object id);"
    out.println ""
    out.println "    /**"
    out.println "     * 通过条件删除"
    out.println "     *"
    out.println "     * @param t"
    out.println "     * @return"
    out.println "     */"
    out.println "    Integer deleteByQuery(@Param(\"t\") T t);"
    out.println ""
    out.println "    /**"
    out.println "     * 全量更新"
    out.println "     *"
    out.println "     * @param t"
    out.println "     * @return"
    out.println "     */"
    out.println "    Integer fullUpdate(T t);"
    out.println ""
    out.println "    /**"
    out.println "     * 增量更新"
    out.println "     *"
    out.println "     * @param t"
    out.println "     * @return"
    out.println "     */"
    out.println "    Integer incUpdate(T t);"
    out.println ""
    out.println "    /**"
    out.println "     * 通过主键查询"
    out.println "     *"
    out.println "     * @param id"
    out.println "     * @return"
    out.println "     */"
    out.println "    T selectByPrimaryKey(@Param(\"id\") Object id);"
    out.println ""
    out.println "    /**"
    out.println "     * 通过条件查询One"
    out.println "     *"
    out.println "     * @param t"
    out.println "     * @return"
    out.println "     */"
    out.println "    T selectOneByQuery(@Param(\"t\") T t);"
    out.println ""
    out.println "    /**"
    out.println "     * 通过条件查询"
    out.println "     *"
    out.println "     * @param t"
    out.println "     * @param start"
    out.println "     * @param end"
    out.println "     * @return"
    out.println "     */"
    out.println "    List<T> selectByQuery(@Param(\"t\") T t, @Param(\"start\") Integer start, @Param(\"end\") Integer end);"
    out.println ""
    out.println "    /**"
    out.println "     * 通过条件查询条数"
    out.println "     *"
    out.println "     * @param t"
    out.println "     * @return"
    out.println "     */"
    out.println "    Integer count(@Param(\"t\") T t);"
    out.println ""
    out.println "}"
}

def mapper(out, baseName, className, tableName, paramName, tableComment, fields) {
    int index = 0
    out.println "package ${packageName}.mapper;"
    out.println ""
    out.println "import ${packageName}.mapper.base.${baseName}BaseMapper;"
    out.println "import ${packageName}.model.${className}Model;"
    out.println "import ${packageName}.domain.Query${className}ListCondition;"
    out.println "import ${packageName}.vo.Query${className}ListDTO;"
    out.println "import org.springframework.stereotype.Repository;"
    out.println ""
    out.println "import java.util.List;"
    out.println ""
    out.println "/**"
    out.println " * @author "
    out.println " */"
    out.println "@Repository"
    out.println "public interface ${className}Mapper extends ${baseName}BaseMapper<${className}Model> {"
    out.println ""
    out.println "    /**"
    out.println "     * 自定义的分页条件查询${tableComment}列表"
    out.println "     *"
    out.println "     * @param condition"
    out.println "     * @return"
    out.println "     */"
    out.println "    List<Query${className}ListDTO> defineQueryList(Query${className}ListCondition condition);"
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
