FROM openjdk:8-jre-slim
COPY target/EfficientHillClimbers-0.20-DPX.jar /root
RUN mkdir -p /data
WORKDIR /data
CMD ["java", "-jar", "/root/EfficientHillClimbers-0.20-DPX.jar"]
ENTRYPOINT ["java", "-jar", "/root/EfficientHillClimbers-0.20-DPX.jar"]
