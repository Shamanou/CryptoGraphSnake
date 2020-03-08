FROM openjdk:15-slim

COPY target/1.0-0.0.1.jar 1.0-0.0.1.jar

ENTRYPOINT ["java","-jar","1.0-0.0.1.jar", "7jZ/a0zsSuceK+QC5JFBLFg2JkmcSMnWa83puC/ZR9MRJJYENVD/7aIk", "4HRFGduARY/hPJrfmRFvBucVn1BVC10QTWK7Q+Bg8b34/ufbGPFkMpqpbuCSM42aL9XWzcG0wgrYRjcgWifhug=="]