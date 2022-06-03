package cn.hutool.core.lang.func;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Array;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
public class LambdaUtilTest {

	@Test
	public void getMethodNameTest() {
		final String methodName = LambdaUtil.getMethodName(MyTeacher::getAge);
		Assert.assertEquals("getAge", methodName);
	}

	@Test
	public void getFieldNameTest() {
		final String fieldName = LambdaUtil.getFieldName(MyTeacher::getAge);
		Assert.assertEquals("age", fieldName);
	}

	@Test
	public void resolveTest() {
		Stream.<Runnable>of(() -> {
			// 引用构造函数
			Func0<MyTeacher> lambda = MyTeacher::new;
			LambdaInfo lambdaInfo = LambdaUtil.resolve(lambda);
			Assert.assertEquals(0, lambdaInfo.getParameterTypes().length);
			Assert.assertEquals(MyTeacher.class, lambdaInfo.getReturnType());
		}, () -> {
			// 数组构造函数引用(此处数组构造参数)
			Func1<Integer, MyTeacher[]> lambda = MyTeacher[]::new;
			LambdaInfo lambdaInfo = LambdaUtil.resolve(lambda);
			Assert.assertEquals(int.class, lambdaInfo.getParameterTypes()[0]);
			Assert.assertEquals(MyTeacher.class, ((Class<Array>) lambdaInfo.getReturnType()).getComponentType());
		}, () -> {
			// 引用静态方法
			Func0<String> noArgsStaticMethod = MyTeacher::takeAge;
			LambdaInfo lambdaInfo = LambdaUtil.resolve(noArgsStaticMethod);
			Assert.assertEquals(String.class, lambdaInfo.getReturnType());
		}, () -> {
			// 引用特定对象的实例方法
			Func0<String> instantiated = new MyTeacher()::getAge;
			LambdaInfo lambdaInfo = LambdaUtil.resolve(instantiated);
			Assert.assertEquals(String.class, lambdaInfo.getReturnType());
		}, () -> {
			// 引用特定类型的任意对象的实例方法
			Func1<MyTeacher, String> annoInstantiated = MyTeacher::getAge;
			LambdaInfo lambdaInfo = LambdaUtil.resolve(annoInstantiated);
			Assert.assertEquals(String.class, lambdaInfo.getReturnType());
		}).forEach(Runnable::run);
	}

	@Test
	public void getRealClassTest() {
		final MyTeacher myTeacher = new MyTeacher();
		Stream.<Runnable>of(() -> {
			// 引用特定类型的任意对象的实例方法
			final Func1<MyTeacher, String> lambda = MyTeacher::getAge;
			Assert.assertEquals(MyTeacher.class, LambdaUtil.getRealClass(lambda));
		}, () -> {
			// 枚举测试，不会导致类型擦除
			final Func1<LambdaKindEnum, Integer> lambda = LambdaKindEnum::ordinal;
			Assert.assertEquals(LambdaKindEnum.class, LambdaUtil.getRealClass(lambda));
		}, () -> {
			// 调用父类方法，能获取到正确的子类类型
			final Func1<MyTeacher, ?> lambda = MyTeacher::getId;
			Assert.assertEquals(MyTeacher.class, LambdaUtil.getRealClass(lambda));
		}, () -> {
			// 引用特定对象的实例方法
			Func0<String> lambda = myTeacher::getAge;
			Assert.assertEquals(MyTeacher.class, LambdaUtil.getRealClass(lambda));
		}, () -> {
			// 枚举测试，只能获取到枚举类型
			Func0<Integer> lambda = LambdaKindEnum.REF_NONE::ordinal;
			Assert.assertEquals(Enum.class, LambdaUtil.getRealClass(lambda));
		}, () -> {
			// 调用父类方法，只能获取到父类类型
			VoidFunc0 lambda = myTeacher::getId;
			Assert.assertEquals(Entity.class, LambdaUtil.getRealClass(lambda));
		}, () -> {
			// 引用静态带参方法，能够获取到正确的参数类型
			Func1<MyTeacher, String> lambda = MyTeacher::takeAgeBy;
			Assert.assertEquals(MyTeacher.class, LambdaUtil.getRealClass(lambda));
		}, () -> {
			// 引用父类静态带参方法，只能获取到父类类型
			Func0<?> lambda = MyTeacher::takeId;
			Assert.assertEquals(Entity.class, LambdaUtil.getRealClass(lambda));
		}, () -> {
			// 引用静态无参方法，能够获取到正确的类型
			Func0<String> lambda = MyTeacher::takeAge;
			Assert.assertEquals(MyTeacher.class, LambdaUtil.getRealClass(lambda));
		}, () -> {
			// 引用父类静态无参方法，能够获取到正确的参数类型
			Func1<MyTeacher, ?> lambda = MyTeacher::takeIdBy;
			Assert.assertEquals(MyTeacher.class, LambdaUtil.getRealClass(lambda));
		}).forEach(Runnable::run);
	}

	@Data
	@AllArgsConstructor
	static class MyStudent {

		private String name;
	}

	@Data
	public static class Entity<T> {

		private T id;

		public static <T> T takeId() {
			return new Entity<T>().getId();
		}

		public static <T> T takeIdBy(final Entity<T> entity) {
			return entity.getId();
		}


	}

	@Data
	@EqualsAndHashCode(callSuper = true)
	static class MyTeacher extends Entity<MyTeacher> {

		public static String takeAge() {
			return new MyTeacher().getAge();
		}

		public static String takeAgeBy(final MyTeacher myTeacher) {
			return myTeacher.getAge();
		}

		public String age;
	}

	/**
	 * 测试Lambda类型枚举
	 */
	enum LambdaKindEnum {
		REF_NONE,
		REF_getField,
		REF_getStatic,
		REF_putField,
		REF_putStatic,
		REF_invokeVirtual,
		REF_invokeStatic,
		REF_invokeSpecial,
		REF_newInvokeSpecial,
	}
}
