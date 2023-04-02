//package tang.xuechengplusbase.base.handler;
//
//import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
//import org.apache.ibatis.reflection.MetaObject;
//import org.springframework.stereotype.Component;
//
//import java.util.Date;
//
//@Component
///**
// * mybatisPlus 自动填充时间和用户id
// */
//public class MyMetaObjectHandler implements MetaObjectHandler {
//    @Override
//    public void insertFill(MetaObject metaObject) {
////        Long userId = null;
////        try {
////            userId = SecurityUtils.getUserId();
////        } catch (Exception e) {
////            e.printStackTrace();
////            userId = -1L;//表示是自己创建
////        }
//        this.setFieldValByName("createDate", new Date(), metaObject);
////        this.setFieldValByName("createBy",userId , metaObject);
//        this.setFieldValByName("changeDate", new Date(), metaObject);
////        this.setFieldValByName("updateBy", userId, metaObject);
//    }
//
//    @Override
//    public void updateFill(MetaObject metaObject) {
//        this.setFieldValByName("changeDate", new Date(), metaObject);
////        this.setFieldValByName("updateBy", SecurityUtils.getUserId(), metaObject);
//    }
//}
