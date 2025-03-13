#!/usr/bin/env python3

import sys
import pandas as pd
import matplotlib.pyplot as plt
from pathlib import Path

def parse_metrics_file(filename):
    metrics = []
    
    with open(filename, 'r') as f:
        for line in f:
            line = line.strip()
            if line.startswith('>'):
                parts = line[1:].split()
                if len(parts) >= 7:
                    metrics.append({
                        'alignment': parts[0],
                        'sensitivity': float(parts[2]),
                        'specificity': float(parts[3]),
                        'coverage': float(parts[4]),
                        'mean_shift_error': float(parts[5])
                    })
    
    return pd.DataFrame(metrics)

def create_visualizations(df, output_dir):
    plt.style.use('default')

    output_dir = Path(output_dir)
    output_dir.mkdir(exist_ok=True)

    plt.figure(figsize=(10, 6))
    metrics = ['sensitivity', 'specificity', 'coverage', 'mean_shift_error']
    df_melted = df.melt(value_vars=metrics)
    plt.boxplot([df[metric] for metric in metrics], labels=metrics)
    plt.title('Distribution of Alignment Metrics')
    plt.xticks(rotation=45)
    plt.tight_layout()
    plt.savefig(output_dir / 'metrics_boxplot.png')
    plt.close()

    plt.figure(figsize=(8, 6))
    corr = df[metrics].corr()
    plt.imshow(corr, cmap='coolwarm', aspect='equal')
    plt.colorbar()
    plt.xticks(range(len(metrics)), metrics, rotation=45)
    plt.yticks(range(len(metrics)), metrics)
    for i in range(len(metrics)):
        for j in range(len(metrics)):
            plt.text(j, i, f'{corr.iloc[i, j]:.2f}', 
                    ha='center', va='center')
    plt.title('Correlation between Metrics')
    plt.tight_layout()
    plt.savefig(output_dir / 'correlation_heatmap.png')
    plt.close()

    fig, axes = plt.subplots(2, 2, figsize=(15, 15))
    axes = axes.ravel()

    axes[0].scatter(df['sensitivity'], df['specificity'])
    axes[0].set_xlabel('Sensitivity')
    axes[0].set_ylabel('Specificity')
    axes[0].set_title('Sensitivity vs Specificity')

    axes[1].scatter(df['sensitivity'], df['coverage'])
    axes[1].set_xlabel('Sensitivity')
    axes[1].set_ylabel('Coverage')
    axes[1].set_title('Sensitivity vs Coverage')

    axes[2].scatter(df['specificity'], df['coverage'])
    axes[2].set_xlabel('Specificity')
    axes[2].set_ylabel('Coverage')
    axes[2].set_title('Specificity vs Coverage')

    axes[3].scatter(df['mean_shift_error'], df['sensitivity'])
    axes[3].set_xlabel('Mean Shift Error')
    axes[3].set_ylabel('Sensitivity')
    axes[3].set_title('Mean Shift Error vs Sensitivity')
    
    plt.tight_layout()
    plt.savefig(output_dir / 'scatter_plots.png')
    plt.close()

def generate_html_report(df, output_dir):
    html_content = f"""
    <!DOCTYPE html>
    <html>
    <head>
        <title>Alignment Metrics Report</title>
        <style>
            body {{ font-family: Arial, sans-serif; margin: 20px; }}
            .container {{ max-width: 1200px; margin: 0 auto; }}
            .section {{ margin-bottom: 30px; }}
            table {{ border-collapse: collapse; width: 100%; margin-bottom: 20px; }}
            th, td {{ border: 1px solid #ddd; padding: 8px; text-align: left; }}
            th {{ background-color: #f2f2f2; }}
            img {{ max-width: 100%; height: auto; margin: 20px 0; }}
        </style>
    </head>
    <body>
        <div class="container">
            <h1>Alignment Metrics Report</h1>
            
            <div class="section">
                <h2>Summary Statistics</h2>
                {df.describe().to_html()}
            </div>
            
            <div class="section">
                <h2>Visualizations</h2>
                <h3>Distribution of Metrics</h3>
                <img src="metrics_boxplot.png" alt="Metrics Boxplot">
                
                <h3>Correlation Heatmap</h3>
                <img src="correlation_heatmap.png" alt="Correlation Heatmap">
                
                <h3>Scatter Plots</h3>
                <img src="scatter_plots.png" alt="Scatter Plots">
            </div>
            
            <div class="section">
                <h2>Detailed Results</h2>
                {df.to_html(index=False)}
            </div>
        </div>
    </body>
    </html>
    """
    
    with open(output_dir / 'metrics_report.html', 'w') as f:
        f.write(html_content)

def main():
    if len(sys.argv) != 3:
        print("Usage: python visualize_metrics.py <metrics_file> <output_dir>")
        sys.exit(1)
    
    metrics_file = sys.argv[1]
    output_dir = sys.argv[2]

    df = parse_metrics_file(metrics_file)

    create_visualizations(df, output_dir)

    generate_html_report(df, output_dir)
    
    print(f"Report generated in {output_dir}/metrics_report.html")

if __name__ == "__main__":
    main() 