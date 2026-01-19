# blink-framework-validation

针对数据检验封装，提供了统一的数据范围校验,主要中请求DTO中使用提供类联动校验逻辑注解是对hibernate-validator的补充
后续可根据业务，进行参考自定义校验逻辑注解

细粒度考量：之所以将validation划分为一个单独的模块，是为了api模块 即业务应用服务提供的对外调用服务（feign或其他rpc）能够
最小依赖的引入，避免引入web-starter模块了

## @DataDict 

name 对应为数据表sys_data_dict中的dict_name 缓存组件通过注解上配置的name得到数据字典缓存对象
然后根据数据字典上配置的数据范围进行校验 实现详情[DataDictConstraintValidator](src/main/java/com/blink/framework/validate/validator/DataDictConstraintValidator.java)

使用示例 
```java
    /**
     * 角色id
     */
    @NotNull
    @DataDict(name="systemId",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private Integer roleId;


    /**
     * 角色名称
     */
    @NotNull
    @DataDict(name="systemName",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String roleName;


    /**
     * 角色英文名称
     */
    @DataDict(name="systemEnName",message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
    private String roleEnName;

```

注解[DataDict](src/main/java/com/blink/framework/validate/annotation/DataDict.java)


## 联动校验
多个字段在校验上存在联动逻辑
目前提供了以下注解
 [ConditionalRequired](src/main/java/com/blink/framework/validate/annotation/ConditionalRequired.java)
 [IsDate](src/main/java/com/blink/framework/validate/annotation/IsDate.java)
[MutuallyExclusive](src/main/java/com/blink/framework/validate/annotation/MutuallyExclusive.java)
 [NonNegative](src/main/java/com/blink/framework/validate/annotation/NonNegative.java)
 [SameValue](src/main/java/com/blink/framework/validate/annotation/NonNegative.java)
[StartEndDate](src/main/java/com/blink/framework/validate/annotation/StartEndDate.java)


 其他实现
 
1、 @ScriptAssert 在JDK17中需要引入额外的依赖 
  如果使用JDK8-14，默认有Nashorn引擎。在JDK15及以上，Nashorn被移除，blink不考虑使用

2、@AssertTrue
    可以使用
```java
// 必须命名为isXXX，这样Bean Validation会自动识别
    @AssertTrue(message = "密码和确认密码必须一致")
    public boolean isPasswordMatching() {
        return password != null && password.equals(confirmPassword);
    }

    // 或者使用其他名称，但在注解中指定
    // @AssertTrue(message = "密码和确认密码必须一致")
    // public boolean isValidPassword() {
    //     return password != null && password.equals(confirmPassword);
    // }
```

3、使用 Spring 的 Validator 接口
 可以使用 不够优雅
```java
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class BookingDTOValidator implements Validator {
    
    @Override
    public boolean supports(Class<?> clazz) {
        return BookingDTO.class.isAssignableFrom(clazz);
    }
    
    @Override
    public void validate(Object target, Errors errors) {
        BookingDTO dto = (BookingDTO) target;
        
        // 校验日期范围
        if (dto.getStartDate() != null && dto.getEndDate() != null) {
            if (!dto.getStartDate().isBefore(dto.getEndDate())) {
                errors.rejectValue("endDate", "date.range.invalid", 
                    "结束日期必须晚于开始日期");
            }
            
            // 校验预订时长不能超过30天
            long daysBetween = java.time.temporal.ChronoUnit.DAYS
                .between(dto.getStartDate(), dto.getEndDate());
            if (daysBetween > 30) {
                errors.reject("booking.duration.exceeded", 
                    "预订时长不能超过30天");
            }
        }
        
        // 校验人数逻辑
        if (dto.getAdultCount() != null && dto.getChildCount() != null) {
            int total = dto.getAdultCount() + dto.getChildCount();
            if (total > 10) {
                errors.rejectValue("adultCount", "guests.exceeded",
                    "总人数不能超过10人");
            }
            if (dto.getChildCount() > 0 && dto.getAdultCount() == 0) {
                errors.rejectValue("adultCount", "adult.required",
                    "有儿童时必须至少有一位成人");
            }
        }
    }
}
```
