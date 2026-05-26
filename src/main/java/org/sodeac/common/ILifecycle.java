/*******************************************************************************
 * Copyright (c) 2019 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common;

public interface ILifecycle
{
    interface ILifecycleEvent
    {
        ILifecycleState getContemporaryState();
    }
    
    interface IHealthCheckType { }
    
    String getId();
    
    String getType();
    
    ILifecycleState getContemporaryState();
    
    interface ILifecycleState
    {
        ILifecycle getLifecycle();
        
        interface IUninstalledState extends ILifecycleState
        {
            interface IOnUninstallEvent extends ILifecycleEvent { }
        }
        
        interface IInstalledState extends IUninstalledState, ILifecycleState
        {
            interface IOnInstallEvent extends ILifecycleEvent { }
        }
        
        interface IStartUpState extends IInstalledState, ILifecycleState
        {
            interface IOnStartUpEvent extends ILifecycleEvent { }
            
            interface IConfigureEvent extends ILifecycleEvent { }
            
            interface ICheckRequirement extends ILifecycleEvent { } // TODO remove, do automatically
            
            interface IBootstrapEvent extends ILifecycleEvent { }
            
            interface ILoadStateEvent extends ILifecycleEvent { }
            
            interface IHealthCheckOnStartUpEvent extends ILifecycleEvent, IHealthCheckType { }
        }
        
        interface IBrokenState extends IStartUpState, ILifecycleState
        {
            interface IOnBrokenEvent extends ILifecycleEvent { }
            
            interface IHealingEvent extends ILifecycleEvent { }
            
            interface IHealthCheckOnBrokenEvent extends ILifecycleEvent, IHealthCheckType { }
        }
        
        interface IReadyState extends IStartUpState, ILifecycleState
        {
            interface IOnReadyEvent extends ILifecycleEvent { }
            
            interface IHealthCheckOnReadyEvent extends ILifecycleEvent, IHealthCheckType { }
            
            interface IStartIOEvent extends ILifecycleEvent { }
        }
        
        interface IActiveState extends IReadyState, ILifecycleState
        {
            interface IOnActiveEvent extends ILifecycleEvent { }
            
            interface IHealthCheckOnActiveEvent extends ILifecycleEvent, IHealthCheckType { }
            
            interface IStopIOEvent extends ILifecycleEvent { }
        }
        
        interface IShutDownState extends IStartUpState, ILifecycleState
        {
            interface IOnShutDownEvent extends ILifecycleEvent { }
            
            interface ISaveStateEvent extends ILifecycleEvent { }
            
            interface IDisposeEvent extends ILifecycleEvent { }
        }
    }
}
