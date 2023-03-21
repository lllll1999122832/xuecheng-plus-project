package tang.xuechengplusbase.base.utils;

import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

public class BeanCopyUtils {
    private BeanCopyUtils(){

    }

    /**
     * 采用泛型拷贝
     * @param source
     * @param clazz
     * @return
     * @param <T>
     */

    public static <T>T copyBean(Object source,Class<T> clazz){
        T result=null;
        try {
            result=clazz.newInstance();
            BeanUtils.copyProperties(source,result);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
    //使用流的方式装换,定义两个泛型
    public static <O,T>List<T> copyBeanList(List<O> list,Class<T> clazz){
        return list.stream()
                .map(o -> copyBean(o, clazz))
                .collect(Collectors.toList());
    }



}
