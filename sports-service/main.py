from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import nfl_data_py as nfl
import pandas as pd
import uvicorn
from typing import List, Dict, Any, Optional

app = FastAPI(title="Personal Dashboard Sports Service")

# Configure CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Helper to calculate standings from schedule
def calculate_standings(year: int):
    # Import schedule
    games = nfl.import_schedules([year])
    
    # Filter for completed games or games with results
    games = games[games['result'].notna()]
    
    standings = {}
    
    # Initialize teams
    teams = set(games['home_team'].unique()) | set(games['away_team'].unique())
    for team in teams:
        standings[team] = {'wins': 0, 'losses': 0, 'ties': 0, 'points_for': 0, 'points_against': 0}
        
    for _, game in games.iterrows():
        home = game['home_team']
        away = game['away_team']
        home_score = game['home_score']
        away_score = game['away_score']
        
        # Update points
        standings[home]['points_for'] += home_score
        standings[home]['points_against'] += away_score
        standings[away]['points_for'] += away_score
        standings[away]['points_against'] += home_score
        
        # Update records
        res = game['result'] # home - away
        if res > 0:
            standings[home]['wins'] += 1
            standings[away]['losses'] += 1
        elif res < 0:
            standings[home]['losses'] += 1
            standings[away]['wins'] += 1
        else:
            standings[home]['ties'] += 1
            standings[away]['ties'] += 1
            
    # Add team metadata
    team_desc = nfl.import_team_desc()
    team_map = team_desc.set_index('team_abbr')[['team_conf', 'team_division']].to_dict('index')
    
    results = []
    for team, stats in standings.items():
        # Handle some abbreviation mapping issues if necessary
        # nfl_data_py usually uses standard abbrs (CHI, GB, etc)
        meta = team_map.get(team, {'team_conf': 'Unknown', 'team_division': 'Unknown'})
        
        total_games = stats['wins'] + stats['losses'] + stats['ties']
        win_pct = (stats['wins'] + 0.5 * stats['ties']) / total_games if total_games > 0 else 0
        
        results.append({
            'team_name': team,
            'wins': stats['wins'],
            'losses': stats['losses'],
            'ties': stats['ties'],
            'points_for': stats['points_for'],
            'points_against': stats['points_against'],
            'win_pct': win_pct,
            'conference': meta.get('team_conf'),
            'division': meta.get('team_division')
        })
        
    return results

@app.get("/")
def read_root():
    return {"message": "Sports Service is running"}

@app.get("/api/sports/standings/{year}")
def get_standings(year: int):
    try:
        data = calculate_standings(year)
        return {"data": data}
    except Exception as e:
        print(f"Error calculating standings: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/sports/bears/summary/{year}")
def get_bears_summary(year: int):
    try:
        data = calculate_standings(year)
        bears = next((t for t in data if t['team_name'] == 'CHI'), None)
        
        # Fallback if CHI not found (e.g. no games played yet)
        if not bears:
             bears = {
                'team_name': 'CHI', 
                'wins': 0, 'losses': 0, 'ties': 0, 
                'points_for': 0, 'points_against': 0, 'win_pct': 0
             }
             
        return {"data": bears}
    except Exception as e:
        print(f"Error fetching Bears summary: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/sports/bears/roster/{year}")
def get_bears_roster(year: int):
    try:
        df = nfl.import_seasonal_rosters([year])
        
        # Filter for CHI
        bears_df = df[df['team'] == 'CHI']
        
        cols_to_keep = ['player_name', 'position', 'jersey_number', 'status', 'college', 'years_exp', 'headshot_url']
        
        # Ensure columns exist
        available_cols = [c for c in cols_to_keep if c in bears_df.columns]
        bears_df = bears_df[available_cols].fillna('')
        
        records = bears_df.to_dict(orient="records")
        return {"data": records}
    except Exception as e:
        print(f"Error fetching Bears roster: {e}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
