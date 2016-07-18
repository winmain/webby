package webby.commons.system.mbean;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

/**
 * Стандартный регистратор MBean-ов.
 *
 * @author den
 */
//@Singleton
public class CommonMBeanRegistrar implements MBeanRegistrar {

    private final String baseDomain;
    private final MBeanServer server;
    private final List<String> registeredNames;

    public CommonMBeanRegistrar(String baseDomain) {
        this.baseDomain = baseDomain;
        this.server = ManagementFactory.getPlatformMBeanServer();
        this.registeredNames = new ArrayList<>();
    }

    @Override
    public <T> MBeanRegBuilder register(T impl, Class<T> mbeanInterface) {
        try {
            return new CommonMBeanRegBuilder(new AnnotatedStandardMBean(impl, mbeanInterface));
        } catch (NotCompliantMBeanException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public MBeanRegBuilder register(Object object) {
        try {
            return new CommonMBeanRegBuilder(new AnnotatedStandardMBean(object));
        } catch (NotCompliantMBeanException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void unregisterAll() {
        for (String registeredName : registeredNames) {
            try {
                server.unregisterMBean(new ObjectName(registeredName));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class CommonMBeanRegBuilder implements MBeanRegBuilder {

        private final Object object;

        public CommonMBeanRegBuilder(Object object) {
            this.object = object;
        }

        @Override
        public void withName(String type) {
            register(baseDomain + ":type=" + type);
        }

        @Override
        public void withName(String type, String name) {
            register(baseDomain + ":type=" + type + ",name=" + name);
        }

        @Override
        public void withFullName(String fullName) {
            register(fullName);
        }

        private void register(String fullName) {
            try {
                server.registerMBean(object, new ObjectName(fullName));
                registeredNames.add(fullName);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
