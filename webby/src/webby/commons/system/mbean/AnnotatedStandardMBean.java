package webby.commons.system.mbean;


import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import javax.management.*;

/**
 * Обёртка над MBean-объектом, которая осуществляет поддержку аннотаций {@link Description},
 * {@link PName}. Таким образом, MBean получает описание методов и наименование параметров для
 * visualvm.
 * <p>
 * Пример:
 *
 * <pre>
 * ManagementFactory.getPlatformMBeanServer().registerMBean(
 *                 new AnnotatedStandardMBean(this, AutoPatcherMBean.class),
 *                 new ObjectName(&quot;com.xwars:type=AutoPatcher&quot;));
 * </pre>
 *
 * Или упрощенный случай, когда интерфейс MBean не известен:
 *
 * <pre>
 * ManagementFactory.getPlatformMBeanServer().registerMBean(
 *                 new AnnotatedStandardMBean(this),
 *                 new ObjectName(&quot;com.xwars:type=AutoPatcher&quot;));
 * </pre>
 *
 * Т.е., достаточно обернуть регистрируемый MBean этим классом.
 *
 * @author den
 */
public class AnnotatedStandardMBean extends StandardMBean {

    /** Instance where the MBean interface is implemented by another object. */
    public <T> AnnotatedStandardMBean(T impl, Class<T> mbeanInterface)
            throws NotCompliantMBeanException {
        super(impl, mbeanInterface);
    }

    /** Instance where the MBean interface is implemented by another object. */
    public <T> AnnotatedStandardMBean(T impl)
            throws NotCompliantMBeanException {
        super(impl, null);
    }

    /** Instance where the MBean interface is implemented by this object. */
    protected AnnotatedStandardMBean(Class<?> mbeanInterface)
            throws NotCompliantMBeanException {
        super(mbeanInterface);
    }

    @Override
    protected String getDescription(MBeanOperationInfo op) {
        Method m = methodFor(getMBeanInterface(), op);
        return descriptionFor(op, m);
    }

    @Override
    protected String getDescription(MBeanAttributeInfo attr) {
        Method g = getterFor(getMBeanInterface(), attr);
        return descriptionFor(attr, g);
    }

    @Override
    protected String getParameterName(MBeanOperationInfo op,
                                      MBeanParameterInfo param,
                                      int paramNo) {
        String name = param.getName();
        Method m = methodFor(getMBeanInterface(), op);
        if (m != null) {
            PName pname = getParameterAnnotation(m, paramNo, PName.class);
            if (pname != null) {
                name = pname.value();
            }
        }
        return name;
    }

    // ------------------------------- Static methods -------------------------------

    static Class<?> classForName(String name, ClassLoader loader)
            throws ClassNotFoundException {
        Class<?> c = primitiveClasses.get(name);
        if (c == null) {
            c = Class.forName(name, false, loader);
        }
        return c;
    }

    private static final Map<String, Class<?>> primitiveClasses = new HashMap<String, Class<?>>();

    static {
        Class<?>[] prims = {
                byte.class, short.class, int.class, long.class,
                float.class, double.class, char.class, boolean.class,
        };
        for (Class<?> c : prims) {
            primitiveClasses.put(c.getName(), c);
        }
    }

    private static Method getterFor(Class<?> mbeanInterface, MBeanAttributeInfo attr) {
        String prefix = attr.isIs() ? "is" : "get";
        String name = prefix + attr.getName();

        return findMethod(mbeanInterface, name);
    }

    private static Method methodFor(Class<?> mbeanInterface, MBeanOperationInfo op) {
        final MBeanParameterInfo[] params = op.getSignature();
        final String[] paramTypes = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            paramTypes[i] = params[i].getType();
        }

        return findMethod(mbeanInterface, op.getName(), paramTypes);
    }

    private static String descriptionFor(MBeanFeatureInfo feature, Method m) {
        if (m != null) {
            Description d = m.getAnnotation(Description.class);
            if (d != null) {
                return d.value();
            }
        }
        return feature.getDescription();
    }

    private static Method findMethod(Class<?> mbeanInterface, String name, String... paramTypes) {
        try {
            final ClassLoader loader = mbeanInterface.getClassLoader();
            final Class<?>[] paramClasses = new Class<?>[paramTypes.length];
            for (int i = 0; i < paramTypes.length; i++) {
                paramClasses[i] = classForName(paramTypes[i], loader);
            }
            return mbeanInterface.getMethod(name, paramClasses);
        } catch (RuntimeException e) {
            // avoid accidentally catching unexpected runtime exceptions
            throw e;
        } catch (Exception e) {
            return null;
        }
    }

    static <A extends Annotation> A getParameterAnnotation(Method m, int paramNo, Class<A> annot) {
        for (Annotation a : m.getParameterAnnotations()[paramNo]) {
            if (annot.isInstance(a)) {
                return annot.cast(a);
            }
        }
        return null;
    }
}