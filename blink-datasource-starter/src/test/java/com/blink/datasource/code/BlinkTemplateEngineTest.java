package com.blink.datasource.code;

import com.baomidou.mybatisplus.generator.config.builder.CustomFile;
import com.baomidou.mybatisplus.generator.config.po.TableInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * BlinkTemplateEngine 单元测试
 * 验证代码生成器的自定义模板引擎逻辑
 *
 * @author binblink
 * @since 2026-04-12
 */
@DisplayName("BlinkTemplateEngine 单元测试")
class BlinkTemplateEngineTest {

    private final BlinkTemplateEngine engine = new BlinkTemplateEngine();

    // ==================== getFileName 方法测试 ====================

    @Nested
    @DisplayName("getFileName 方法测试")
    class GetFileNameTest {

        @Test
        @DisplayName("TC-001: Req文件名生成-dto.req包-Add")
        void getFileName_whenReqPackage_shouldGenerateReqFileName() throws Exception {
            // given
            TableInfo tableInfo = createTableInfo("SysUserDO");
            CustomFile file = createCustomFile("dto.req", "templates/AddReq.java.vm");

            // when
            String result = invokeGetFileName(engine, tableInfo, file);

            // then
            assertThat(result).isEqualTo("AddSysUserReq.java");
        }

        @Test
        @DisplayName("TC-002: Req文件名生成-dto.req包-Delete")
        void getFileName_whenReqPackageDelete_shouldGenerateDeleteReqFileName() throws Exception {
            // given
            TableInfo tableInfo = createTableInfo("SysUserDO");
            CustomFile file = createCustomFile("dto.req", "templates/DeleteReq.java.vm");

            // when
            String result = invokeGetFileName(engine, tableInfo, file);

            // then
            assertThat(result).isEqualTo("DeleteSysUserReq.java");
        }

        @Test
        @DisplayName("TC-003: Req文件名生成-dto.req包-Query")
        void getFileName_whenReqPackageQuery_shouldGenerateQueryReqFileName() throws Exception {
            // given
            TableInfo tableInfo = createTableInfo("SysUserDO");
            CustomFile file = createCustomFile("dto.req", "templates/QueryReq.java.vm");

            // when
            String result = invokeGetFileName(engine, tableInfo, file);

            // then
            assertThat(result).isEqualTo("QuerySysUserReq.java");
        }

        @Test
        @DisplayName("TC-004: Req文件名生成-dto.req包-Update")
        void getFileName_whenReqPackageUpdate_shouldGenerateUpdateReqFileName() throws Exception {
            // given
            TableInfo tableInfo = createTableInfo("SysUserDO");
            CustomFile file = createCustomFile("dto.req", "templates/UpdateReq.java.vm");

            // when
            String result = invokeGetFileName(engine, tableInfo, file);

            // then
            assertThat(result).isEqualTo("UpdateSysUserReq.java");
        }

        @Test
        @DisplayName("TC-005: Rsp文件名生成-dto.rsp包")
        void getFileName_whenRspPackage_shouldGenerateRspFileName() throws Exception {
            // given
            TableInfo tableInfo = createTableInfo("SysUserDO");
            CustomFile file = createCustomFile("dto.rsp", "templates/QueryRsp.java.vm");

            // when
            String result = invokeGetFileName(engine, tableInfo, file);

            // then
            assertThat(result).isEqualTo("QuerySysUserRsp.java");
        }

        @Test
        @DisplayName("TC-006: Test文件名生成")
        void getFileName_whenTestPackage_shouldGenerateTestFileName() throws Exception {
            // given
            TableInfo tableInfo = createTableInfo("SysUserDO");
            CustomFile file = createCustomFile("test", "templates/test.java.vm");

            // when
            String result = invokeGetFileName(engine, tableInfo, file);

            // then
            assertThat(result).isEqualTo("SysUserControllerTest.java");
        }

        @Test
        @DisplayName("TC-007: DO后缀移除")
        void getFileName_whenEntityHasDOSuffix_shouldRemoveDO() throws Exception {
            // given
            TableInfo tableInfo = createTableInfo("SysUserDO");
            CustomFile file = createCustomFile("dto.req", "templates/AddReq.java.vm");

            // when
            String result = invokeGetFileName(engine, tableInfo, file);

            // then
            assertThat(result).doesNotContain("DO");
            assertThat(result).isEqualTo("AddSysUserReq.java");
        }

        @Test
        @DisplayName("TC-008: 其他包名")
        void getFileName_whenOtherPackage_shouldReturnDefaultFileName() throws Exception {
            // given
            TableInfo tableInfo = createTableInfo("SysUserDO");
            CustomFile file = createCustomFile("other", "templates/Other.java.vm");

            // when
            String result = invokeGetFileName(engine, tableInfo, file);

            // then
            assertThat(result).isEqualTo(".java");
        }

        @Test
        @DisplayName("TC-009: 实体名无DO后缀")
        void getFileName_whenEntityNoDOSuffix_shouldUseOriginalName() throws Exception {
            // given
            TableInfo tableInfo = createTableInfo("SysUser");
            CustomFile file = createCustomFile("dto.req", "templates/AddReq.java.vm");

            // when
            String result = invokeGetFileName(engine, tableInfo, file);

            // then
            assertThat(result).isEqualTo("AddSysUserReq.java");
        }

        @Test
        @DisplayName("TC-010: DetailRsp文件名生成")
        void getFileName_whenDetailRsp_shouldGenerateCorrectly() throws Exception {
            // given
            TableInfo tableInfo = createTableInfo("SysDeptDO");
            CustomFile file = createCustomFile("dto.rsp", "templates/DetailRsp.java.vm");

            // when
            String result = invokeGetFileName(engine, tableInfo, file);

            // then
            assertThat(result).isEqualTo("DetailSysDeptRsp.java");
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 通过反射调用 getFileName 方法
     */
    private String invokeGetFileName(BlinkTemplateEngine engine, TableInfo tableInfo, CustomFile file) throws Exception {
        Method method = BlinkTemplateEngine.class.getDeclaredMethod("getFileName", TableInfo.class, CustomFile.class);
        method.setAccessible(true);
        return (String) method.invoke(engine, tableInfo, file);
    }

    /**
     * 创建模拟 TableInfo
     */
    private TableInfo createTableInfo(String entityName) {
        TableInfo tableInfo = mock(TableInfo.class);
        when(tableInfo.getEntityName()).thenReturn(entityName);
        return tableInfo;
    }

    /**
     * 创建模拟 CustomFile
     */
    private CustomFile createCustomFile(String packageName, String templatePath) {
        return new CustomFile.Builder()
                .packageName(packageName)
                .templatePath(templatePath)
                .fileName("temp.java")
                .build();
    }
}
