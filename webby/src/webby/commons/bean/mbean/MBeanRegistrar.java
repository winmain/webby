package webby.commons.bean.mbean;

/**
 * Регистратор MBean'ов.<br>
 * Стандартная реализация этого интерфейса является заглушкой. Она нужна, чтобы тесты не валились
 * при попытке зарегистировать один и тот же класс.
 *
 * @author den
 */
//@ImplementedBy(DummyMBeanRegistrar.class)
public interface MBeanRegistrar {

    <T> MBeanRegBuilder register(T impl, Class<T> mbeanInterface);

    MBeanRegBuilder register(Object object);

    void unregisterAll();

    interface MBeanRegBuilder {
        /**
         * Зарегистрировать mbean по имени "rosrabota:type={type}"
         */
        void withName(String type);

        /**
         * Зарегистрировать mbean по имени "rosrabota:type={type},name={name}"
         */
        void withName(String type, String name);

        /**
         * Зарегистрировать mbean по имени "{fullname}"
         */
        void withFullName(String fullName);
    }

    class DummyMBeanRegistrar implements MBeanRegistrar {

        @Override
        public <T> MBeanRegBuilder register(T impl, Class<T> mbeanInterface) {
            return new DummyMBeanRegBuilder();
        }

        @Override
        public MBeanRegBuilder register(Object object) {
            return new DummyMBeanRegBuilder();
        }

        @Override
        public void unregisterAll() {
        }
    }

    class DummyMBeanRegBuilder implements MBeanRegBuilder {

        @Override
        public void withName(String type) {
        }

        @Override
        public void withName(String type, String name) {
        }

        @Override
        public void withFullName(String fullName) {
        }
    }
}
