apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: ce-clusterrole
rules:
  - apiGroups:
      - ""
    resources:
      - pods
      - nodes
      - nodes/proxy
      - events
      - namespaces
      - persistentvolumes
      - persistentvolumeclaims
    verbs:
      - get
      - list
      - watch
  - apiGroups:
      - apps
      - extensions
    resources:
      - statefulsets
      - deployments
      - daemonsets
      - replicasets
    verbs:
      - get
      - list
      - watch
  - apiGroups:
      - batch
    resources:
      - jobs
      - cronjobs
    verbs:
      - get
      - list
      - watch
  - apiGroups:
      - metrics.k8s.io
    resources:
      - pods
      - nodes
    verbs:
      - get
      - list
  - apiGroups:
      - storage.k8s.io
    resources:
      - storageclasses
    verbs:
      - get
      - list
      - watch

---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: ce-clusterrolebinding
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: ce-clusterrole
subjects:
  - kind: ServiceAccount
    name: ${serviceAccountName}
    namespace: ${serviceAccountNamespace}