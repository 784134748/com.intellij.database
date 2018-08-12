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

packageName = "com.sample"
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
    def tableName = table.getName()
    def className = javaName(table.getName(), true)
    def fields = calcFields(table)
    new File(dir, className + "Mapper.xml").withPrintWriter { out -> generate(out, tableName, className, fields) }
}

def generate(out, tableName, className, fields) {
    int index = 0
    out.println "<?xml version='1.0' encoding='UTF-8' ?>"
    out.println "<!DOCTYPE mapper PUBLIC '-//mybatis.org//DTD Mapper 3.0//EN' 'http://mybatis.org/dtd/mybatis-3-mapper.dtd' >"
    out.println "<mapper namespace='${packageName}.mapper.${className}Mapper'>"
    out.println "    <sql id='Base_Column_List' >"
    out.print "    "
    fields.each() {
        if (index != 0) {
            out.print ", "
        }
        out.print "${it.right}"
        index++
    }
    out.println ""
    out.println "    </sql>"
    out.println "    <select id='selectByPrimaryKey' resultType='${packageName}.model.${className}' parameterType='java.lang.Long'>"
    out.println "        select "
    out.println "        <include refid= 'Base_Column_List' />"
    out.println "        from ${tableName} "
    out.println "        where id = #{id}"
    out.println "    </select>"
    out.println ""
    out.println "    <select id='selectByQuery' resultType='${packageName}.model.${className}' parameterType='java.util.Map'>"
    out.println "        select "
    out.println "        <include refid= 'Base_Column_List' />"
    out.println "        from ${tableName} "
    out.println "        <where>"
    out.println "            <include refid='query_filter'/>"
    out.println "        </where>"
    out.println "        <if test='start != null'>"
    out.println "            LIMIT #{start},#{limit}"
    out.println "        </if>"
    out.println "    </select>"
    out.println ""
    out.println "    <delete id='deleteByPrimaryKey' parameterType='java.lang.Long'>"
    out.println "        delete from ${tableName} where id = #{id}"
    out.println "    </delete>"
    out.println ""
    out.println "    <select id='count' resultType='java.lang.Integer' parameterType='java.util.Map'>"
    out.println "        select count(*) from ${tableName}"
    out.println "        <where>"
    out.println "            <include refid='query_filter'/>"
    out.println "        </where>"
    out.println "    </select>"
    out.println ""
    out.println "    <insert id='insert' parameterType='java.util.Map' useGeneratedKeys='true' keyProperty='id'>"
    out.println "        insert into ${tableName}"
    out.println "        <trim prefix='(' suffix=')' suffixOverrides=','>"
    fields.each() {
        out.println "                <if test='${it.left} != null'>${it.right},</if>"
    }
    out.println "        </trim>"
    out.println "        <trim prefix='values (' suffix=')' suffixOverrides=','>"
    fields.each() {
        out.println "                <if test='${it.left} != null'>#{${it.left}},</if>"
    }
    out.println "        </trim>"
    out.println "    </insert>"
    out.println ""
    out.println "    <update id='update' parameterType='java.util.Map'>"
    out.println "        update ${tableName}"
    out.println "        <set>"
    fields.each() {
        out.println "                <if test='${it.left} != null'>${it.right} = #{${it.left}},</if>"
    }
    out.println "        </set>"
    out.println "        where id = #{id}"
    out.println "    </update>"
    out.println ""
    out.println "    <sql id='query_filter'>"
    fields.each() {
        out.println "            <if test='${it.left} != null'>and ${it.right} = #{${it.left}}</if>"
    }
    out.println "    </sql>"
    out.println ""
    out.println ""
    out.println "</mapper>"
}


def calcFields(table) {
    DasUtil.getColumns(table).reduce([]) { fields, col ->
        def spec = Case.LOWER.apply(col.getDataType().getSpecification())
        def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
        fields += [[
                           left   : javaName(col.getName(), false),
                           right  : col.getName(),
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

def tableName(str, capitalize) {
    def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it).capitalize() }
            .join("")
            .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
    capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
}
