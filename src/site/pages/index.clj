(ns site.pages.index
  (:require
   [site.ui.home :as home]))

(def articles
  [{:title       "Securing NGO Communications: A Practical Guide"
    :date        "2023-04-18"
    :slug        "securing-ngo-communications"
    :author      "Casey Link"
    :description "A comprehensive approach to implementing secure communication channels for non-profit organizations operating in challenging environments."}
   {:title       "Designing Data Pipelines for Humanitarian Work"
    :date        "2023-02-10"
    :slug        "data-pipelines-humanitarian-work"
    :author      "Casey Link"
    :description "How to build reliable, scalable data processing systems that help humanitarian organizations make better decisions in the field."}
   {:title       "Open Source Contributions: KDE Journey"
    :date        "2022-11-15"
    :slug        "open-source-kde-journey"
    :author      "Casey Link"
    :description "Reflecting on fifteen years of contributing to KDE, the challenges faced, lessons learned, and the evolving landscape of open source desktop environments."}
   {:title       "Mobile Security for Field Workers"
    :date        "2022-08-23"
    :slug        "mobile-security-field-workers"
    :author      "Casey Link"
    :description "Practical strategies for maintaining digital security for NGO staff working in regions with active surveillance and limited connectivity."}
   {:title       "Building Resilient Systems in Resource-Constrained Environments"
    :date        "2022-07-05"
    :slug        "resilient-systems-constrained-environments"
    :author      "Casey Link"
    :description "Technical approaches to developing software that remains functional in environments with unreliable power, limited internet access, and hardware constraints."}])

(defn index [_]
  {:title   "Casey Link | Developer, Technical Strategist & NGO Specialist"
   :uri     "/"
   :content (home/home {:articles articles})})
