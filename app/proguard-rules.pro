# مكتبات AndroidX (Room / WorkManager / Compose / DataStore) تحمل قواعد consumer خاصة بها،
# فلا حاجة لـ -keep class androidx.** الذي كان يُبطل فائدة التصغير.
-keepattributes SourceFile,LineNumberTable
