package com.mix.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 离线解析全部 mapper XML，不需要数据库、不需要启动 Spring。
 * <p>
 * 关键：仅有的两个单元测试不构建 SqlSessionFactory，mapper XML 从未被解析过——
 * 曾出现 <sql id="X"> 内部被误改成 <include refid="X"/> 的自引用，
 * mvn test / compile 全绿，直到 spring-boot:run 构建 SqlSessionFactory 时
 * XMLIncludeTransformer 无限递归 StackOverflowError 才暴露（后端直接起不来）。
 * 本测试让 XML 在测试阶段就被 MyBatis 真正展开一次，把这类错误挡在提交前。
 */
class MapperXmlParseTest {

    @Test
    void allMapperXmlShouldParseWithoutRecursion() throws Exception {
        Resource[] files = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:com/mix/mapper/*.xml");
        assertTrue(files.length > 0, "classpath 下未找到任何 mapper XML，检查 resources 路径");

        for (Resource file : files) {
            // 每个文件独立 Configuration，避免 namespace / sqlFragments 互相污染
            Configuration configuration = new Configuration();
            try (InputStream in = file.getInputStream()) {
                XMLMapperBuilder builder = new XMLMapperBuilder(
                        in, configuration, file.getFilename(), configuration.getSqlFragments());
                builder.parse(); // 会展开 <include>：自引用会在此抛 StackOverflowError
            }
        }
    }
}
