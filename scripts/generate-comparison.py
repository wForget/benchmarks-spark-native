# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
# Copy from https://github.com/apache/datafusion-benchmarks/blob/main/scripts/generate-comparison.py

import argparse
import json
import matplotlib.pyplot as plt
import numpy as np
import re

def geomean(data):
    return np.prod(data) ** (1 / len(data))

def generate_query_speedup_chart(baseline, comparison, label1: str, label2: str, benchmark: str, title: str):
    results = []
    for query in query_names(baseline):
        a = np.median(np.array(baseline[query]))
        b = np.median(np.array(comparison[query]))
        if a > b:
            speedup = a/b-1
        else:
            speedup = -(1/(a/b)-1)
        results.append((query, round(speedup*100, 0)))

    results = sorted(results, key=lambda x: -x[1])

    queries, speedups = zip(*results)

    # Create figure and axis
    if benchmark == "tpch":
        fig, ax = plt.subplots(figsize=(10, 6))
    else:
        fig, ax = plt.subplots(figsize=(35, 10))

    # Create bar chart
    bars = ax.bar(queries, speedups, color='skyblue')

    # Add text annotations
    for bar, speedup in zip(bars, speedups):
        yval = bar.get_height()
        if yval >= 0:
            ax.text(bar.get_x() + bar.get_width() / 2.0, min(800, yval+5), f'{yval:.0f}%', va='bottom', ha='center', fontsize=8,
                    color='blue', rotation=90)
        else:
            ax.text(bar.get_x() + bar.get_width() / 2.0, yval, f'{yval:.0f}%', va='top', ha='center', fontsize=8,
                    color='blue', rotation=90)

    # Add title and labels
    ax.set_title(label2 + " speedup over " + label1 + " (" + title + ")")
    ax.set_ylabel('Speedup (100% speedup = 2x faster)')
    ax.set_xlabel('Query')

    # Customize the y-axis to handle both positive and negative values better
    ax.axhline(0, color='black', linewidth=0.8)
    min_value = (min(speedups) // 100) * 100
    max_value = ((max(speedups) // 100) + 1) * 100 + 50
    if benchmark == "tpch":
        ax.set_ylim(min_value, max_value)
    else:
        # TODO improve this
        ax.set_ylim(-250, 300)

    # Show grid for better readability
    ax.yaxis.grid(True)

    # Save the plot as an image file
    plt.savefig(f'{benchmark}_queries_speedup_{label2}.png', format='png')


def generate_query_comparison_chart(results, labels, benchmark: str, title: str):
    queries = query_names(results[0])
    benches = []
    for _ in results:
        benches.append([])
    for query in queries:
        for i in range(0, len(results)):
            benches[i].append(np.median(np.array(results[i][str(query)])))

    # Define the width of the bars
    bar_width = 0.3

    # Define the positions of the bars on the x-axis
    index = np.arange(len(queries)) * 1.5

    # Create a bar chart
    if benchmark == "tpch":
        fig, ax = plt.subplots(figsize=(15, 6))
    else:
        fig, ax = plt.subplots(figsize=(35, 6))

    for i in range(0, len(results)):
        bar = ax.bar(index + i * bar_width, benches[i], bar_width, label=labels[i])

    # Add labels, title, and legend
    ax.set_title(title)
    ax.set_xlabel('Queries')
    ax.set_ylabel('Query Time (seconds)')
    ax.set_xticks(index + bar_width / 2)
    ax.set_xticklabels(queries)
    ax.legend()

    # Save the plot as an image file
    plt.savefig(f'{benchmark}_queries_compare.png', format='png')

def generate_summary(results, labels, benchmark: str, title: str):
    timings = []
    for _ in results:
        timings.append(0)

    queries = query_names(results[0])
    for query in queries:
        for i in range(0, len(results)):
            timings[i] += np.median(np.array(results[i][str(query)]))

    # Create figure and axis
    fig, ax = plt.subplots()

    # Add title and labels
    ax.set_title(title)
    ax.set_ylabel(f'Time in seconds to run all {len(queries)} {benchmark} queries (lower is better)')

    times = [round(x,0) for x in timings]

    # Create bar chart
    bars = ax.bar(labels, times, color='skyblue')

    # Add text annotations
    for bar in bars:
        yval = bar.get_height()
        ax.text(bar.get_x() + bar.get_width() / 2.0, yval, f'{yval}', va='bottom')  # va: vertical alignment

    plt.savefig(f'{benchmark}_allqueries.png', format='png')

def query_names(result):
    return sorted(list(result.keys()), key=lambda x: int(re.search(r'\d+', x).group()))

def main(files, labels, benchmark: str, title: str):
    results = []
    for filename in files:
        with open(filename) as f:
            results.append(json.load(f))
    generate_summary(results, labels, benchmark, title)
    generate_query_comparison_chart(results, labels, benchmark, title)

    baseline = results[0]
    base_label = labels[0]
    for i in range(1, len(results)):
        generate_query_speedup_chart(baseline, results[i], base_label, labels[i], benchmark, title)

if __name__ == '__main__':
    argparse = argparse.ArgumentParser(description='Generate comparison')
    argparse.add_argument('filenames', nargs='+', type=str, help='JSON result files')
    argparse.add_argument('--labels', nargs='+', type=str, help='Labels')
    argparse.add_argument('--benchmark', type=str, help='Benchmark name (tpch or tpcds)')
    argparse.add_argument('--title', type=str, help='Chart title')
    args = argparse.parse_args()
    main(args.filenames, args.labels, args.benchmark, args.title)