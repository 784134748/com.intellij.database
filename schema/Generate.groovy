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
baseMapprePath = "com.aidynamic.tj.core.base.BaseMapper"
baseModePath = "com.aidynamic.tj.core.base.BaseModel"
idProperties = ["id"] as String[]
gmtCreate = ["gmt_create"] as String[]
gmtModified = ["gmt_modified"] as String[]
isDeleteProperties = ["is_delete"] as String[]
delete = '\${@com.aidynamic.tj.core.enums.DeleteStatusEnum@YES.getKey()}'
not_delete = '\${@com.aidynamic.tj.core.enums.DeleteStatusEnum@NO.getKey()}'
javaTypeMapping = [
        (~/(?i)bigint/)           : "Long",
        (~/(?i)int|timestamp/)    : "Integer",
        (~/(?i)float|double|real/): "java.lang.Double",
        (~/(?i)decimal/)          : "java.math.BigDecimal",
        (~/(?i)datetime/)         : "java.time.LocalDateTime",
        (~/(?i)date/)             : "java.time.LocalDate",
        (~/(?i)time/)             : "java.time.LocalTime",
        (~/(?i)json/)             : "String",
        (~/(?i)/)                 : "String"
]

parameterTypeMapping = [
        (~/(?i)bigint/)           : "java.lang.Long",
        (~/(?i)int|timestamp/)    : "java.lang.Integer",
        (~/(?i)float|double|real/): "java.lang.Double",
        (~/(?i)decimal/)          : "java.math.BigDecimal",
        (~/(?i)datetime/)         : "java.time.LocalDateTime",
        (~/(?i)date/)             : "java.time.LocalDate",
        (~/(?i)time/)             : "java.time.LocalTime",
        (~/(?i)json/)             : "java.lang.String",
        (~/(?i)/)                 : "java.lang.String"
]

dataTypeMapping = [
        (~/(?i)bigint/)           : "Long",
        (~/(?i)int|timestamp/)    : "Integer",
        (~/(?i)float|double|real/): "Double",
        (~/(?i)decimal/)          : "BigDecimal",
        (~/(?i)datetime/)         : "LocalDateTime",
        (~/(?i)date/)             : "LocalDate",
        (~/(?i)time/)             : "LocalTime",
        (~/(?i)json/)             : "String",
        (~/(?i)/)                 : "String"
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
    def baseName = ""
    def className = javaName(table.getName(), true)
    def paramName = javaName(table.getName(), false)
    def fields = calcFields(table)
    def tableName = table.getName()
    def tableComment = table.getComment()


    //创建mapper文件夹
    def mapperDir = dir.toString() + sepa + "mapper" + sepa
    def mapperPath = new File(mapperDir)
    mapperPath.mkdirs()
    //创建Mapper.java
    def mapperFile = new File(mapperDir, className + "Mapper.java")
    if (!mapperFile.exists()) {
        mapperFile.withPrintWriter { out -> mapper(out, baseName, className, tableName, paramName, tableComment, fields) }
    }


    //创建model文件夹
    def modelDir = dir.toString() + sepa + "model" + sepa
    def modelPath = new File(modelDir)
    modelPath.mkdirs()
    //创建Model.java
    def modelFile = new File(modelDir, className + "Model.java")
    modelFile.withPrintWriter { out -> model(out, baseName, className, tableName, paramName, tableComment, fields) }


    //创建xml文件夹
    def xmlDir = dir.toString().substring(0, index + 10) + sepa + "resources" + sepa + "mapper" + sepa
    def xmlPath = new File(xmlDir)
    xmlPath.mkdirs()
    //创建xml文件
    xmlFile = new File(xmlDir, className + "Mapper.xml")
    xmlFileTmp = new File(xmlDir, className + "Mapper-tmp.xml")
    if (!xmlFile.exists()) {
        xmlFile.withPrintWriter { out -> xml(out, baseName, className, tableName, paramName, tableComment, fields) }
        BufferedReader reader = new BufferedReader(new FileReader(xmlFile))
        xmlFileTmp.withPrintWriter { out -> replace(reader, out, baseName, className, tableName, paramName, tableComment, fields) }
        xmlFile.delete()
        xmlFileTmp.renameTo(xmlFile)
    } else {
        BufferedReader reader = new BufferedReader(new FileReader(xmlFile))
        xmlFileTmp.withPrintWriter { out -> replace(reader, out, baseName, className, tableName, paramName, tableComment, fields) }
        xmlFile.delete()
        xmlFileTmp.renameTo(xmlFile)
    }

}

def mapper(out, baseName, className, tableName, paramName, tableComment, fields) {
    out.println "package ${packageName}.mapper;"
    out.println ""
    out.println "import ${baseMapprePath};"
    out.println "import ${packageName}.model.${className}Model;"
    out.println "import org.apache.ibatis.annotations.Mapper;"
    out.println "import org.springframework.stereotype.Repository;"
    out.println ""
    out.println "import java.util.List;"
    out.println ""
    out.println "/**"
    out.println " * @author "
    out.println " */"
    out.println "@Mapper"
    out.println "@Repository"
    out.println "public interface ${className}Mapper extends ${baseName}BaseMapper<${className}Model> {"
    out.println ""
    out.println "}"
}

def model(out, baseName, className, tableName, paramName, tableComment, fields) {
    out.println "package ${packageName}.model;"
    out.println ""
    out.println "import ${baseModePath};"
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
    out.println "@EqualsAndHashCode(callSuper = true)"
    out.println "@ApiModel(value = \"${className}Model\", description = \"${tableComment}\")"
    out.println "public class ${className}Model extends ${baseName}BaseModel implements Serializable {"
    out.println ""
    out.println "    public static final long serialVersionUID = 1L;"
    out.println ""
    fields.each() {
        if (it.javaType.equals("java.time.LocalDateTime")) {
            out.println "    @ApiModelProperty(value = \"${it.comment}\", dataType = \"${it.dataType}\", example = \"2019-10-01 00:00:00\")"
            out.println "    @DateTimeFormat(pattern = \"yyyy-MM-dd HH:mm:ss\")"
            out.println "    @JsonFormat(pattern = \"yyyy-MM-dd HH:mm:ss\")"
        } else if (it.javaType.equals("java.time.LocalDate")) {
            out.println "    @ApiModelProperty(value = \"${it.comment}\", dataType = \"${it.dataType}\", example = \"2019-10-01\")"
            out.println "    @DateTimeFormat(pattern = \"yyyy-MM-dd\")"
            out.println "    @JsonFormat(pattern = \"yyyy-MM-dd\")"
        } else if (it.javaType.equals("java.time.LocalTime")) {
            out.println "    @ApiModelProperty(value = \"${it.comment}\", dataType = \"${it.dataType}\", example = \"00:00:00\")"
            out.println "    @DateTimeFormat(pattern = \"HH:mm:ss\")"
            out.println "    @JsonFormat(pattern = \"HH:mm:ss\")"
        } else {
            out.println "    @ApiModelProperty(value = \"${it.comment}\", dataType = \"${it.dataType}\")"
        }
        out.println "    private ${it.javaType} ${it.javaName};"
        out.println ""
    }
    out.println "}"
}

def xml(out, baseName, className, tableName, paramName, tableComment, fields) {
    out.println "<?xml version='1.0' encoding='UTF-8' ?>"
    out.println "<!DOCTYPE mapper PUBLIC '-//mybatis.org//DTD Mapper 3.0//EN' 'http://mybatis.org/dtd/mybatis-3-mapper.dtd' >"
    out.println "<mapper namespace='${packageName}.mapper.${className}Mapper'>"
    out.println ""
    out.println "    <!--一串华丽的分割线,分割线内禁止任何形式的修改-->"
    out.println "    <!--一串华丽的分割线,分割线内禁止任何形式的修改-->"
    out.println ""
    out.println "</mapper>"
}

def replace(reader, out, baseName, className, tableName, paramName, tableComment, fields) {
    int index = 0
    boolean ignore = false
    int time = 0
    String line
    list = []
    while ((line = reader.readLine()) != null) {
        if (ignore == false) {
            out.println "${line}"
        }
        if (line.contains("    <!--一串华丽的分割线,分割线内禁止任何形式的修改-->")) {
            ignore = !ignore
            time += 1
            if (time == 1) {
                /**
                 * BaseResultMap
                 */

                out.println ""
                out.println "    <resultMap id='BaseResultMap' type='${packageName}.model.${className}Model'>"
                out.println "        <constructor>"
                fields.each() {
                    if (propertiesContainField(it, idProperties)) {
                        out.println "            <idArg column='${it.colName}' javaType='${it.parameterType}'/>"
                    } else {
                        out.println "            <arg column='${it.colName}' javaType='${it.parameterType}'/>"
                    }
                }
                out.println "        </constructor>"
                fields.each() {
                    out.println "        <result property='${it.javaName}' column='${it.colName}'/>"
                }
                out.println "    </resultMap>"

                /**
                 * Base_Column_List
                 */

                out.println ""
                out.println "    <sql id='Base_Column_List'>"
                out.print "    "
                fields.each() {
                    if (index != 0) {
                        out.print ", "
                    }
                    out.print "${tableName}.`${it.colName}`"
                    index++
                }
                out.println ""
                out.println "    </sql>"

                /**
                 * selectByPrimaryKey
                 */

                out.println ""
                out.println "    <select id='selectByPrimaryKey' resultMap='BaseResultMap'"
                fields.each() {
                    if (propertiesContainField(it, idProperties)) {
                        out.println "            parameterType='${it.parameterType}'>"
                    }
                }
                out.println "        select "
                out.println "        <include refid='Base_Column_List'/>"
                out.println "        from ${tableName} "
                out.println "        where `id` = #{id}"
                out.println "    </select>"

                /**
                 * batchSelectByPrimaryKey
                 */

                out.println ""
                out.println "    <select id='batchSelectByPrimaryKey' resultMap='BaseResultMap' parameterType='ArrayList'>"
                out.println "        select "
                out.println "        <include refid='Base_Column_List'/>"
                out.println "        from ${tableName} "
                out.println "        where `id` in"
                out.println "        <foreach collection='list' item='id' index='index' open='(' close=')' separator=','>"
                out.println "            #{id}"
                out.println "        </foreach>"
                out.println "    </select>"

                /**
                 * selectOneByQuery
                 */

                out.println ""
                out.println "    <select id='selectOneByQuery' resultMap='BaseResultMap'"
                out.println "            parameterType='${packageName}.model.${className}Model'>"
                out.println "        select "
                out.println "        <include refid='Base_Column_List'/>"
                out.println "        from ${tableName} "
                out.println "        <where>"
                out.println "            <include refid='query_filter'/>"
                out.println "        </where>"
                out.println "        Limit 1"
                out.println "    </select>"

                /**
                 * selectByQuery
                 */

                out.println ""
                out.println "    <select id='selectByQuery' resultMap='BaseResultMap'>"
                out.println "        select "
                out.println "        <include refid='Base_Column_List'/>"
                out.println "        from ${tableName} "
                out.println "        <where>"
                out.println "            <include refid='query_filter'/>"
                out.println "        </where>"
                out.println "        <if test='start != null and end != null'>"
                out.println "            limit #{start}, #{end}"
                out.println "        </if>"
                out.println "    </select>"

                /**
                 * deleteByPrimaryKey
                 */

                out.println ""
                if (fieldsContainProperties(isDeleteProperties, fields)) {
                    fields.each() {
                        if (propertiesContainField(it, idProperties)) {
                            out.println "    <update id='deleteByPrimaryKey' parameterType='${it.parameterType}'>"
                        }
                    }
                    out.println "        update ${tableName} set `${isDeleteProperties[0]}` = ${delete} where `id` = #{id}"
                    out.println "    </update>"
                    out.println ""
                } else {
                    fields.each() {
                        if (propertiesContainField(it, idProperties)) {
                            out.println "    <delete id='deleteByPrimaryKey' parameterType='${it.parameterType}'>"
                        }
                    }
                    out.println "        delete from ${tableName} where `id` = #{id}"
                    out.println "    </delete>"
                }

                /**
                 * deleteByQuery
                 */

                if (fieldsContainProperties(isDeleteProperties, fields)) {
                    out.println ""
                    out.println "    <update id='deleteByQuery' parameterType='${packageName}.model.${className}Model'>"
                    out.println "        update ${tableName} set `${isDeleteProperties[0]}` = ${delete}"
                    out.println "        <where>"
                    out.println "            <include refid='query_filter'/>"
                    out.println "        </where>"
                    out.println "    </update>"
                    out.println ""
                } else {
                    out.println "    <delete id='deleteByQuery' parameterType='${packageName}.model.${className}Model'>"
                    out.println "        delete from ${tableName}"
                    out.println "        <where>"
                    out.println "            <include refid='query_filter'/>"
                    out.println "        </where>"
                    out.println "    </delete>"
                }

                /**
                 * count
                 */

                out.println ""
                out.println "    <select id='count' resultType='java.lang.Integer' parameterType='${packageName}.model.${className}Model'>"
                out.println "        select count(*) from ${tableName}"
                out.println "        <where>"
                out.println "            <include refid='query_filter'/>"
                out.println "        </where>"
                out.println "    </select>"

                /**
                 * insert
                 */

                out.println ""
                out.println "    <insert id='insert' parameterType='${packageName}.model.${className}Model' useGeneratedKeys='true' keyProperty='id'>"
                out.println "        insert into ${tableName}"
                out.println "        <trim prefix='(' suffix=')' suffixOverrides=','>"
                fields.each() {
                    if (propertiesContainField(it, gmtCreate)) {
                        out.println "            `${it.colName}`,"
                    } else if (propertiesContainField(it, gmtModified)) {
                        out.println "            `${it.colName}`,"
                    } else if (propertiesContainField(it, isDeleteProperties)) {
                        out.println "            `${it.colName}`,"
                    } else if (it.javaType == "String") {
                        out.println "            <if test='${it.javaName} != null'>`${it.colName}`,</if>"
                    } else {
                        out.println "            <if test='${it.javaName} != null'>`${it.colName}`,</if>"
                    }
                }
                out.println "        </trim>"
                out.println "        <trim prefix='values (' suffix=')' suffixOverrides=','>"
                fields.each() {
                    if (propertiesContainField(it, gmtCreate)) {
                        out.println "            now(),"
                    } else if (propertiesContainField(it, gmtModified)) {
                        out.println "            now(),"
                    } else if (propertiesContainField(it, isDeleteProperties)) {
                        out.println "            ${not_delete},"
                    } else {
                        out.println "            <if test='${it.javaName} != null'>#{${it.javaName}},</if>"
                    }
                }
                out.println "        </trim>"
                out.println "    </insert>"

                /**
                 * full_update
                 */

                out.println ""
                out.println "    <update id='fullUpdate' parameterType='${packageName}.model.${className}Model'>"
                out.println "        update ${tableName}"
                out.println "        <set>"
                fields.each() {
                    if (propertiesContainField(it, gmtCreate)) {
                    } else if (propertiesContainField(it, idProperties)) {
                    } else if (propertiesContainField(it, gmtModified)) {
                        out.println "            `${it.colName}` = now(),"
                    } else {
                        out.println "            `${it.colName}` = #{${it.javaName}},"
                    }
                }
                out.println "        </set>"
                out.println "        where id = #{id}"
                out.println "    </update>"

                /**
                 * inc_update
                 */

                out.println ""
                out.println "    <update id='incUpdate' parameterType='${packageName}.model.${className}Model'>"
                out.println "        update ${tableName}"
                out.println "        <set>"
                fields.each() {
                    if (propertiesContainField(it, gmtCreate)) {
                    } else if (propertiesContainField(it, idProperties)) {
                    } else if (propertiesContainField(it, gmtModified)) {
                        out.println "            `${it.colName}` = now(),"
                    } else {
                        out.println "            <if test='${it.javaName} != null'>`${it.colName}` = #{${it.javaName}},</if>"
                    }
                }
                out.println "        </set>"
                out.println "        where `id` = #{id}"
                out.println "    </update>"

                /**
                 * query_filter
                 */

                out.println ""
                out.println "    <sql id='query_filter'>"
                fields.each() {
                    if (propertiesContainField(it, isDeleteProperties)) {
                        out.println "        and `${it.colName}` = ${not_delete}"
                    } else {
                        out.println "        <if test='t.${it.javaName} != null'>and `${it.colName}` = #{t.${it.javaName}}</if>"
                    }
                }
                out.println "    </sql>"
                out.println ""
            }
        }
        if (time == 2) {
            out.println "${line}"
            time += 1
        }
    }
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
        def dataTypeStr = dataTypeMapping.find { p, t -> p.matcher(spec).find() }.value
        fields += [[
                           javaName     : javaName(col.getName(), false),
                           colName      : col.getName(),
                           parameterName: parameterName(col.getName(), true),
                           jdbcType     : jdbcType(col.getDataType()),
                           javaType     : javaTypeStr,
                           parameterType: parameterTypeStr,
                           dataType     : dataTypeStr,
                           comment      : col.getComment()
                   ]]
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

