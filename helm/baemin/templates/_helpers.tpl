{{/*
Common labels attached to every resource in this chart.
*/}}

{{- define "baemin.labels" -}}
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{- end }}

{{- define "baemin.prometheusAnnotations" -}}
prometheus.io/scrape: "true"
prometheus.io/path: "/actuator/prometheus"
prometheus.io/port: {{ . | quote }}
{{- end }}

{{- define "baemin.rollingUpdate" -}}
type: RollingUpdate
rollingUpdate:
  maxSurge: 0
  maxUnavailable: 1
 {{- end }}

{{- define "baemin.backendEnv" -}}
{{- $s := . -}}
- name: SPRING_PROFILES_ACTIVE
  value: "prod"
- name: DB_URL
  valueFrom:
    configMapKeyRef:
      name: db-config
      key: {{ $s.dbKey }}
- name: DB_USERNAME
  valueFrom:
    secretKeyRef:
      name: {{ $s.service }}-secret
      key: DB_USERNAME
- name: DB_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ $s.service }}-secret
      key: DB_PASSWORD
- name: JWT_SECRET
  valueFrom:
    secretKeyRef:
      name: common-secret
      key: JWT_SECRET
- name: {{ $s.hmacKey }}
  valueFrom:
    secretKeyRef:
      name: {{ $s.service }}-secret
      key: {{ $s.hmacKey }}
- name: JAVA_OPTS
  value: {{ $s.javaOpts | quote }}
 {{- end }}

{{- define "baemin.probes" -}}
{{- $port := . -}}
startupProbe:
  httpGet:
    path: /actuator/health/liveness
    port: {{ $port }}
  initialDelaySeconds: 60
  failureThreshold: 120
  periodSeconds: 10
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: {{ $port }}
  initialDelaySeconds: 0
  periodSeconds: 10
  failureThreshold: 3
  timeoutSeconds: 5
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: {{ $port }}
  initialDelaySeconds: 0
  periodSeconds: 10
  failureThreshold: 3
  timeoutSeconds: 5
{{- end }}

{{- define "baemin.clusterIPService" -}}
{{- $s := . -}}
apiVersion: v1
kind: Service
metadata:
  name: {{ $s.name }}
  namespace: baemin
  labels:
    app: {{ $s.name }}
    {{- include "baemin.labels" $s.root | nindent 4 }}
spec:
  type: ClusterIP
  selector:
    app: {{ $s.name }}
  ports:
    - name: http
      port: 80
      targetPort: {{ $s.port }}
{{- end }}

{{- define "baemin.secretMeta" -}}
{{- $s := . -}}
apiVersion: v1
kind: Secret
metadata:
  name: {{ $s.name }}
  namespace: baemin
  labels:
    {{- include "baemin.labels" $s.root | nindent 4 }}
type: Opaque
stringData:
{{- end }}