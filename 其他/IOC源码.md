# SpringIOC源码阅读



## 执行流程

### 1.创建

核心初始化构造器,其他构造器都会回到这个构造器 .

```java
//核心构造器	
public ClassPathXmlApplicationContext(
			String[] configLocations, boolean refresh, @Nullable ApplicationContext parent)
			throws BeansException {

		super(parent);
    	//设置xml中读取的配置信息
		setConfigLocations(configLocations);
		if (refresh) {
            //最重要的方法，初始化容器，布尔类型是判断是否要重新初始化容器
			refresh();
		}
	}
```

### 2.核心流程

refresh方法是容器初始化的核心,下面是核心流程.

```java
	@Override
	public void refresh() throws BeansException, IllegalStateException {
        //防止容器重复启动
		synchronized (this.startupShutdownMonitor) {
			// Prepare this context for refreshing.
            //百度说这个这个方法就是记录启动时间,处理占位符?
			prepareRefresh();

			// Tell the subclass to refresh the internal bean factory.
            //把配置文件中的bean解析成beanDefination,对应为beanName -> beanDefination
			ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

			// Prepare the bean factory for use in this context.
            //主要是设置BeanFactory的类加载器
			prepareBeanFactory(beanFactory);

			try {
				// Allows post-processing of the bean factory in context subclasses.
				postProcessBeanFactory(beanFactory);

				// Invoke factory processors registered as beans in the context.
				invokeBeanFactoryPostProcessors(beanFactory);

				// Register bean processors that intercept bean creation.
				registerBeanPostProcessors(beanFactory);

				// Initialize message source for this context.
				initMessageSource();

				// Initialize event multicaster for this context.
                //初始化事件广播器,我在设计模式文章里写了这块
				initApplicationEventMulticaster();

				// Initialize other special beans in specific context subclasses.
                //模板方法,留给子类初始化自定义bean
				onRefresh();

				// Check for listener beans and register them.
                //注册所有事件的监听器,放在LinkedHashSet集合
				registerListeners();

				// Instantiate all remaining (non-lazy-init) singletons.
                //核心:初始化所有单例bean
				finishBeanFactoryInitialization(beanFactory);

				// Last step: publish corresponding event.
                //广播完成初始化事件new ContextRefreshedEvent(this)
				finishRefresh();
			}

			catch (BeansException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Exception encountered during context initialization - " +
							"cancelling refresh attempt: " + ex);
				}

				// Destroy already created singletons to avoid dangling resources.
				destroyBeans();

				// Reset 'active' flag.
				cancelRefresh(ex);

				// Propagate exception to caller.
				throw ex;
			}

			finally {
				// Reset common introspection caches in Spring's core, since we
				// might not ever need metadata for singleton beans anymore...
				resetCommonCaches();
			}
		}
	}
```

上面是大概流程，下面是对流程每一个方法源码阅读。

#### 1.prepareRefresh()

```java
	/**
	 * Prepare this context for refreshing, setting its startup date and
	 * active flag as well as performing any initialization of property sources.
	 */
	protected void prepareRefresh() {
		// Switch to active.激活起始时间
		this.startupDate = System.currentTimeMillis();
        //设置关闭属性为false , AtomicBoolean类型
		this.closed.set(false);
        //设置激活属性为true，AtomicBoolean类型
		this.active.set(true);

		if (logger.isDebugEnabled()) {
			if (logger.isTraceEnabled()) {
				logger.trace("Refreshing " + this);
			}
			else {
				logger.debug("Refreshing " + getDisplayName());
			}
		}

		// Initialize any placeholder property sources in the context environment.
		initPropertySources();

		// Validate that all properties marked as required are resolvable:
		// see ConfigurablePropertyResolver#setRequiredProperties
        //校验xml配置
		getEnvironment().validateRequiredProperties();

		// Store pre-refresh ApplicationListeners...
		if (this.earlyApplicationListeners == null) {
			this.earlyApplicationListeners = new LinkedHashSet<>(this.applicationListeners);
		}
		else {
			// Reset local application listeners to pre-refresh state.
			this.applicationListeners.clear();
			this.applicationListeners.addAll(this.earlyApplicationListeners);
		}

		// Allow for the collection of early ApplicationEvents,
		// to be published once the multicaster is available...
		this.earlyApplicationEvents = new LinkedHashSet<>();
	}
```

#### 2.obtainFreshBeanFactory()

最重要的方法，初始化BeanFactory，加载bean，注册bean。（未生成bean实例）

```java
	protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
        //关闭旧的beanFactory,创建新的（如果有旧的话）
		refreshBeanFactory();
        //返回刚创建的beanFactory
		return getBeanFactory();
	}

	/**
	 * This implementation performs an actual refresh of this context's underlying
	 * bean factory, shutting down the previous bean factory (if any) and
	 * initializing a fresh bean factory for the next phase of the context's lifecycle.
	 */
	@Override
	protected final void refreshBeanFactory() throws BeansException {
        //销毁老的
		if (hasBeanFactory()) {
			destroyBeans();
			closeBeanFactory();
		}
		try {
            //创建新的DefaultListableBeanFactory
            //DefaultListableBeanFactory继承了大多数BeanFactory的实现类
			//提供获取多个bean，父子分层，自动装配bean等核心功能
			DefaultListableBeanFactory beanFactory = createBeanFactory();
            //序列化id
			beanFactory.setSerializationId(getId());
            //设置是否能循环引用，是否能覆盖bean
			customizeBeanFactory(beanFactory);
            //加载bean到beanFactory
			loadBeanDefinitions(beanFactory);
            //这里Spring之前的版本有锁，我用的5版本没锁，感觉大的方法加锁就已经能保证安全
            //虽然ApplicationContext继承了BeanFactory接口，但感觉可以认为beanFactory的方法是委托			   //给内部的DefaultListableBeanFactory来处理的
			//
			this.beanFactory = beanFactory;
		}
		catch (IOException ex) {
			throw new ApplicationContextException("I/O error parsing bean definition source for " + getDisplayName(), ex);
		}
	}
```

