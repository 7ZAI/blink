# blink-framework-validation

使用 hibernate-validator 针对数据校验封装，提供了统一的字段约束校验，主要在请求 DTO 中使用，提供联动校验逻辑注解。
后续可根据业务，进行参考自定义校验逻辑注解。

细粒度考量：之所以将 validation 划分为一个单独的模块，是为了避免服务间调用封装的 api 模块（如 blink-base-api）引入其他不必要的依赖，这样遵循最小依赖原则。

## @FieldConstraint

name 对应 sys_field_constraint 表中的 constraint_name，缓存组件通过注解上配置的 name 获取字段约束缓存对象，然后根据约束规则进行校验。

实现详情：[FieldConstraintValidator](src/main/java/com/blink/framework/validate/validator/FieldConstraintValidator.java)

使用示例：
```java
/**
 * 角色ID
 */
@NotNull
@FieldConstraint(name = "systemId", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
private Integer roleId;

/**
 * 角色名称
 */
@NotNull
@FieldConstraint(name = "systemName", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
private String roleName;

/**
 * 角色英文名称
 */
@FieldConstraint(name = "systemEnName", message = BaseErrCodeConstant.PARAMETER_OUT_RANGE)
private String roleEnName;
```

注解：[FieldConstraint](src/main/java/com/blink/framework/validate/annotation/FieldConstraint.java)

## 联动校验

多个字段在校验上存在联动逻辑，目前提供了以下注解：

- [ConditionalRequired](src/main/java/com/blink/framework/validate/annotation/ConditionalRequired.java)
- [IsDate](src/main/java/com/blink/framework/validate/annotation/IsDate.java)
- [MutuallyExclusive](src/main/java/com/blink/framework/validate/annotation/MutuallyExclusive.java)
- [NonNegative](src/main/java/com/blink/framework/validate/annotation/NonNegative.java)
- [SameValue](src/main/java/com/blink/framework/validate/annotation/SameValue.java)
- [StartEndDate](src/main/java/com/blink/framework/validate/annotation/StartEndDate.java)

### 其他实现

1. **@ScriptAssert** - 在 JDK17 中需要引入额外的依赖
   如果使用 JDK8-14，默认有 Nashorn 引擎。在 JDK15及以上，Nashorn 被移除，blink 不考虑使用。

2. **@AssertTrue**
   可以使用：
   ```java
   // 必须命名为 isXXX，这样 Bean Validation 会自动识别
   @AssertTrue(message = "密码和确认密码必须一致")
   public boolean isPasswordMatching() {
       return password != null && password.equals(confirmPassword);
   }
   ```

3. **使用 Spring 的 Validator 接口**
   可以使用，但不够优雅：
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