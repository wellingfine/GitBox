# Gson 通过反射按字段名读写 JSON，字段名必须保留：
# data.model 是数据类，data.repo 里还有 ItemsFile / RecordsFile 这类 JSON 包装类。
# 否则 release 包读不出已有数据，写进 git 仓库的 JSON 也会变成混淆后的键名。
-keep class com.v0tx.gitbox.data.model.** { *; }
-keep class com.v0tx.gitbox.data.repo.** { *; }

-keepattributes Signature
-keepattributes *Annotation*

# JGit 5.13 引用了 Android 平台不存在的 JMX / GSSAPI / slf4j 类；
# 本项目只用 HTTP(S)+Token，不走这些路径，但全量 R8 会因此报 Missing classes。
-dontwarn java.lang.management.**
-dontwarn javax.management.**
-dontwarn org.ietf.jgss.**
-dontwarn org.slf4j.**
