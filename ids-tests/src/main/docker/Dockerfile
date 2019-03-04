FROM vasdvp/health-apis-maven:latest

COPY maven/ /sentinel
RUN chmod 755 /sentinel/*sh
ENTRYPOINT ["/sentinel/entrypoint.sh"]
CMD []
