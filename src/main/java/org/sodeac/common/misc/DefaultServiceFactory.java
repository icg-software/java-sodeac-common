package org.sodeac.common.misc;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.function.Function;

import org.sodeac.common.IService.IFactoryEnvironment;

public class DefaultServiceFactory implements Function<IFactoryEnvironment<?, ?>, Object>
{
    @Override
    public Object apply(final IFactoryEnvironment<?, ?> t)
    {
        try
        {
            final Class<?> serviceClass = t.getServiceClass();

            constr:
            for (final Constructor<?> constructor : serviceClass.getDeclaredConstructors())
            {
                // if(!constructor.isAccessible())
                if(!constructor.canAccess(null))
                {
                    continue;
                }

                boolean hasConfigurationParameter = false;
                boolean unknownParameter = false;
                final int index = 0;

                param:
                for (final Type type : constructor.getGenericParameterTypes())
                {
                    if(t.getConfiguration() != null)
                    {
                        if(((Class<?>) type).isInstance(t.getConfiguration()))
                        {
                            hasConfigurationParameter = true;
                            continue param;
                        }
                    }

                    unknownParameter = true;
                }

                if(unknownParameter)
                {
                    continue constr;
                }

                if(hasConfigurationParameter)
                {
                    return constructor.newInstance(t.getConfiguration());
                }
            }

            if(t.isRequireConfiguration())
            {
                return null;
            }

            return t.getServiceClass().getDeclaredConstructor().newInstance();
        }
        catch (final RuntimeException e)
        {
            throw e;
        }
        catch (final Exception | Error e)
        {
            throw new RuntimeWrappedException(e);
        }
    }
}