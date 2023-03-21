//package tang.xuechengplusbase.base.config;
//
//
//import org.springframework.core.convert.converter.Converter;
//import org.springframework.stereotype.Component;
//
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//
///**
// * 自定义格式转换器
// * Stirng 转为Date
// */
//@Component
//public class MyDateConverter implements Converter<String, Date> {
//
//    @Override
//    public Date convert(String s) {
//        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
//        try {
//            return format.parse(s);
//        } catch (ParseException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        return null;
//    }
//}
