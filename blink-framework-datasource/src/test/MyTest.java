import com.blink.datasource.CodeGenerator;
import org.junit.jupiter.api.Test;

/**
 *
 * @Author binblink 
 * @Date 2025/10/14
 */
public class MyTest {

    @Test
    public void test1(){
        CodeGenerator.generateByCustomTemplate("jdbc:mysql://localhost:3306/blink?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=GMT%2B8",
                "root","123456");
    }
}
