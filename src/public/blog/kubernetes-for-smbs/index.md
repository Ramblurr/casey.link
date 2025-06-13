{
    :page/title "Kubernetes for SMBs: When a Scooter Beats a Battleship"
    :blog/date "2018-08-15"
    :blog/modified "2025-06-01"
    :blog/author "Casey Link"
    :blog/description "Say it with me: I will not have Google-scale problems. I have customer-scale problems."
    :blog/tags #{"devops" "systemd" "evergreen"}
}

```clojure
^:embed [:site.ui.alert/Alert {:site.ui.alert/type :info :site.ui.alert/title "Still relevant"}
[:p"This article is over 5 years old, but oh boy, it is as relevant today as it was in 2018. Check the end for a short update."]
[:p "-Casey in 2025"]]
```

Everyone's talking about Kubernetes.
At every conference, in every DevOps Slack channel, in the Orange Pages, the message is clear: if you're not running Kubernetes, you're doing containers wrong.
Well, I'm here to tell you that for most small and medium businesses, Kubernetes is like using a battleship for your daily commute.

Don't get me wrong, containers are fantastic.
They've revolutionized how we deploy software (especially for runtimes like ruby, node, and python, less for the jar/war community).
But somewhere along the way, we confused "using containers" with "needing Google-scale orchestration."

Say it with me: **I will not have Google-scale problems. I have customer-scale problems. [^f1]**

[^f1]: ...and if you do, then you will have enough money to migrate to kubernetes. 

I consult with SMBs and nonprofits on their technical infrastructure.
Here's what their container deployments actually look like:

- 5 to 20 containers running their core applications.
- A \<your favorite>SQL database.
- Maybe Redis for caching.
- They don't have a dedicated ops team. They have a Sarah who knows Linux and Docker pretty well and Jim who's really good with databases.

That's it.

But then they give into the FOMO (after all they don't want to be the dinosaur still using "just Docker"). 
Now here's what happens when they deploy Kubernetes: suddenly they're running 20+ containers just for the infrastructure!
The Kubernetes control plane itself, ingress controllers, storage plugins, network policies, monitoring sidecars... the service mesh *shudder*.
When your infrastructure containers outnumber your actual workloads by 2-to-1, you've got to ask yourself: what problem are we solving here?

The teams I work with typically have 0-5 developers who also handle operations in devops style.
They don't have dedicated SREs.
They push code during business hours and schedule maintenance windows for updates.
Their uptime requirements?
"Don't break during the workday, and please let us sleep through the night."

These aren't the problems Kubernetes was designed to solve.
Kubernetes solves Google problems: thousands of services, millions of requests, teams distributed across the globe.
Most businesses, even successful and growing ones, will never have Google problems.

## The Hidden Tax of Complexity

Matt Rogish from ReactiveOps recently argued that ["Kubernetes has low accidental complexity and high essential complexity"][overkill].
I appreciate this argument, in that [simple doesn't mean easy][simpleeasy], but the "essential complexity" of Kubernetes is still astronomical for most organizations.
Essential complexity isn't inherently virtuous. It's only valuable when it maps to essential problems you actually have.
He dismisses CTOs who say "I just have a Rails application and plain old EC2 VMs will give me what I need" as being short-sighted.

[overkill]: https://web.archive.org/web/20200507004329/https://www.fairwinds.com/blog/is-kubernetes-overkill
[simpleeasy]: https://www.infoq.com/presentations/Simple-Made-Easy/

The "essential complexity" of Kubernetes includes:

* Understanding pods, deployments, services, and ingresses
* Grokking the networking model (ClusterIP vs NodePort vs LoadBalancer)
* Learning YAML templating with Helm or Kustomize (and the absolute mess that managing that is)
* Debugging why your pod is stuck in CrashLoopBackOff
* Figuring out why your PersistentVolumeClaim won't bind
* Understanding RBAC and service accounts
* Keeping up with the regular Kubernetes releases and deprecations

This isn't accidental complexity.
These are fundamental concepts in the Kubernetes model.
But for a team running 10 containers, this essential complexity is solving problems they don't have while creating new ones they didn't ask for.


The real cost is opportunity cost.

While you're wrestling with pod network policies and figuring out why your persistent volumes won't mount, your competitors are shipping features using boring, simple technology that just works.

## A battleship is still a battleship even if you are renting it

I can see the k8s proponotes frothing at the mouth now, "what about managed Kubernetes?"
In the last year Google's launched GKE for a while, Azure AKS last year, and this year AWS has launched EKS.
True, these services remove some operational burden. You don't have to manage the control plane or worry about etcd backups.
But here's the thing: even with managed Kubernetes, you're still on the hook for a lot.

You still need to understand pods, services, deployments, ingress controllers, persistent volumes, and the rest of the Kubernetes abstraction layer.
When your app won't deploy, the error messages assume you speak fluent Kubernetes.
When performance tanks, you're debugging through layers of network policies and service meshes.
You're still managing node pools, scaling policies, resource quotas, network policies, and RBAC configurations.
You're still debugging why your pods are stuck in "ImagePullBackOff" or why your PersistentVolumeClaim won't bind.
Managed or not, you still need to know what a CNI plugin is and why yours is misbehaving.

Rogish admits that most companies don't need Kubernetes for scaling, the one thing it's probably great at.

Rogish's argument is that EC2 instances can't automatically restart crashed applications.
"Apps running on regular EC2 instances have no automatic restart if your Rails application runs out of memory," he writes.


Congratulations, you've reinvented Linux the operating system and systemd but with a thousand more moving parts, and distributed to boot.
A simple `Restart=always` in your systemd service file solves the restart problem.
Or even AWS Auto Scaling Groups (which I am always hesitant to recommend, but for ensuring a certain number of instances is running, is pretty for purpose)? [^f2]
Kubernetes is not a magic out-of-memory-begone! artifact by any means, your nodes can run out of memory just like your single EC2 instance can, and your pods will being going up-and-down as your k8s scheduler thrashes around.

[^f2]: And speaking of memory management, there's exciting work happening in userspace OOM handling right now.
Facebook's been developing sophisticated out-of-memory daemons that can proactively manage memory pressure before your app even crashes.
We're moving beyond the kernel's reactive OOM killer to intelligent userspace solutions.

[oomd]: https://web.archive.org/web/20201112015706/https://engineering.fb.com/2018/07/19/production-engineering/oomd/


## Simple Alternatives That Actually Work

Here's the thing: you probably already have everything you need.
Docker plus systemd can handle most single-host deployments beautifully.
Write a systemd unit file, enable it, and you're done.
Need to update?
`docker pull`, `systemd restart`.
It's so simple it feels like cheating.

For multi-container applications, docker-compose gives you 80% of what Kubernetes offers with about 10% of the complexity.
Define your services in a YAML file, run `docker-compose up`, and watch your stack come alive.
Need horizontal scaling?
Run haproxy or nginx on a box as a load balancer, run several other boxes running Docker. 
Your favorite monitoring package.
It's not fancy, but it works.

And sometimes, a bash script that pulls a new container and restarts it is all you need.
I've seen this "architecture" quietly mint millions in revenue without breaking a sweat.
It's understandable, debuggable, and maintainable by anyone who knows basic Linux.


### The Boring Path Forward

[Dan McKinley's "Choose Boring Technology"][boring] remains my north star for tech-stack and infrastructure decisions.
Every company gets about three innovation tokens.
Spend them wisely.
If your core business is e-commerce, why spend an innovation token on orchestration?
Use boring tools for infrastructure so you can be innovative where it matters: your actual product.

Start with the simplest thing that could possibly work.
Measure actual pain points before adding complexity.
Feel the pain of manual deployments before automating.
Hit scaling limits before building for infinite scale.
You might be surprised how far simple solutions can take you.

If you eventually need Kubernetes, it'll be obvious.
You'll have specific problems that simpler tools can't solve.
Your team will have grown.
You'll have budget for dedicated operations.
When that day comes, yea sure, start with a managed service like GKE or AKS.
The migration will make sense because it solves real problems, not theoretical ones.


[boring]: https://web.archive.org/web/20180806233940/https://mcfunley.com/choose-boring-technology
