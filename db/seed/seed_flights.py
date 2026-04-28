"""
Requirements: pip install psycopg2-binary pandas python-dotenv
"""

import os
import uuid
import random
from datetime import datetime, timedelta
import pandas as pd
import psycopg2
from psycopg2.extras import execute_values
from dotenv import load_dotenv

load_dotenv()

# ── DB connection ─────────────────────────────────────────────
conn = psycopg2.connect(
    host=os.getenv("FLIGHT_DB_HOST", "localhost"),
    port=os.getenv("FLIGHT_DB_PORT", "5434"),
    dbname=os.getenv("FLIGHT_DB_NAME", "flight_db"),
    user=os.getenv("FLIGHT_DB_USER", "flight_user"),
    password=os.getenv("FLIGHT_DB_PASSWORD", "flight_secret_pass"),
)
conn.autocommit = False
cur = conn.cursor()

DATA_DIR = os.path.join(os.path.dirname(__file__), "data")

COUNTRIES = [
    ("United States", "US", "North America", "USD"),
    ("Turkey",        "TR", "Europe",        "TRY"),
    ("Germany",       "DE", "Europe",        "EUR"),
    ("France",        "FR", "Europe",        "EUR"),
    ("United Kingdom","GB", "Europe",        "GBP"),
    ("Japan",         "JP", "Asia",          "JPY"),
    ("UAE",           "AE", "Asia",          "AED"),
    ("Singapore",     "SG", "Asia",          "SGD"),
    ("Australia",     "AU", "Oceania",       "AUD"),
    ("Canada",        "CA", "North America", "CAD"),
    ("Netherlands",   "NL", "Europe",        "EUR"),
    ("Spain",         "ES", "Europe",        "EUR"),
    ("South Korea",   "KR", "Asia",          "KRW"),
    ("Brazil",        "BR", "South America", "BRL"),
]

# city_name, iata_code, timezone, country_iso, lat, lon
CITIES = [
    ("New York",       "NYC", "America/New_York",      "US",  40.7128, -74.0060),
    ("Istanbul",       "IST", "Europe/Istanbul",       "TR",  41.0082,  28.9784),
    ("Berlin",         "BER", "Europe/Berlin",         "DE",  52.5200,  13.4050),
    ("Paris",          "CDG", "Europe/Paris",          "FR",  48.8566,   2.3522),
    ("London",         "LHR", "Europe/London",         "GB",  51.5074,  -0.1278),
    ("Tokyo",          "NRT", "Asia/Tokyo",            "JP",  35.6762, 139.6503),
    ("Dubai",          "DXB", "Asia/Dubai",            "AE",  25.2048,  55.2708),
    ("Singapore",      "SIN", "Asia/Singapore",        "SG",   1.3521, 103.8198),
    ("Los Angeles",    "LAX", "America/Los_Angeles",   "US",  34.0522, -118.2437),
    ("Toronto",        "YYZ", "America/Toronto",       "CA",  43.6532, -79.3832),
    ("Amsterdam",      "AMS", "Europe/Amsterdam",      "NL",  52.3676,   4.9041),
    ("Madrid",         "MAD", "Europe/Madrid",         "ES",  40.4168,  -3.7038),
    ("Seoul",          "ICN", "Asia/Seoul",            "KR",  37.5665, 126.9780),
    ("Frankfurt",      "FRA", "Europe/Berlin",         "DE",  50.1109,   8.6821),
    ("Sydney",         "SYD", "Australia/Sydney",      "AU", -33.8688, 151.2093),
]

# airport_name, iata_code, city_iata, terminal_count
AIRPORTS = [
    ("John F. Kennedy Intl",      "JFK", "NYC", 6),
    ("LaGuardia Airport",         "LGA", "NYC", 4),
    ("Istanbul Airport",          "IST", "IST", 2),
    ("Sabiha Gokcen Intl",        "SAW", "IST", 1),
    ("Berlin Brandenburg Intl",   "BER", "BER", 2),
    ("Charles de Gaulle Intl",    "CDG", "CDG", 3),
    ("Orly Airport",              "ORY", "CDG", 2),
    ("Heathrow Airport",          "LHR", "LHR", 5),
    ("Gatwick Airport",           "LGW", "LHR", 2),
    ("Narita Intl",               "NRT", "NRT", 3),
    ("Haneda Airport",            "HND", "NRT", 4),
    ("Dubai Intl",                "DXB", "DXB", 3),
    ("Changi Airport",            "SIN", "SIN", 4),
    ("Los Angeles Intl",          "LAX", "LAX", 9),
    ("Toronto Pearson Intl",      "YYZ", "YYZ", 2),
    ("Amsterdam Schiphol",        "AMS", "AMS", 1),
    ("Madrid Barajas Intl",       "MAD", "MAD", 4),
    ("Incheon Intl",              "ICN", "ICN", 2),
    ("Frankfurt Intl",            "FRA", "FRA", 2),
    ("Sydney Kingsford Smith",    "SYD", "SYD", 3),
]

# (origin_iata, dest_iata, airline_code, airline_name, duration_min, base_price)
ROUTE_TEMPLATES = [
    ("JFK", "IST", "TK", "Turkish Airlines",     600, 450.00),
    ("IST", "JFK", "TK", "Turkish Airlines",     620, 480.00),
    ("IST", "CDG", "TK", "Turkish Airlines",     225, 180.00),
    ("CDG", "IST", "AF", "Air France",           230, 195.00),
    ("JFK", "LHR", "AA", "American Airlines",    415, 380.00),
    ("LHR", "JFK", "BA", "British Airways",      430, 395.00),
    ("JFK", "FRA", "LH", "Lufthansa",            470, 420.00),
    ("FRA", "BER", "LH", "Lufthansa",             65, 95.00),
    ("BER", "CDG", "LH", "Lufthansa",            115, 140.00),
    ("CDG", "BER", "AF", "Air France",           120, 145.00),
    ("IST", "BER", "TK", "Turkish Airlines",     195, 160.00),
    ("BER", "IST", "TK", "Turkish Airlines",     200, 165.00),
    ("DXB", "SIN", "EK", "Emirates",             420, 280.00),
    ("SIN", "NRT", "SQ", "Singapore Airlines",   360, 310.00),
    ("LHR", "DXB", "EK", "Emirates",             390, 350.00),
    ("DXB", "JFK", "EK", "Emirates",             840, 680.00),
    ("LAX", "NRT", "JL", "Japan Airlines",       660, 580.00),
    ("NRT", "LAX", "JL", "Japan Airlines",       590, 560.00),
    ("YYZ", "LHR", "AC", "Air Canada",           435, 420.00),
    ("AMS", "JFK", "KL", "KLM",                  475, 430.00),
    ("JFK", "AMS", "KL", "KLM",                  460, 410.00),
    ("FRA", "DXB", "LH", "Lufthansa",            360, 330.00),
    ("ICN", "SIN", "OZ", "Asiana Airlines",      360, 290.00),
    ("SYD", "SIN", "QF", "Qantas",               480, 380.00),
    ("MAD", "LHR", "IB", "Iberia",               150, 160.00),
]

AIRLINES = {
    "TK": "Turkish Airlines",
    "LH": "Lufthansa",
    "AA": "American Airlines",
    "BA": "British Airways",
    "AF": "Air France",
    "EK": "Emirates",
    "SQ": "Singapore Airlines",
    "JL": "Japan Airlines",
    "AC": "Air Canada",
    "KL": "KLM",
    "OZ": "Asiana Airlines",
    "QF": "Qantas",
    "IB": "Iberia",
}

AIRCRAFT = ["B737", "A320", "B777", "A380", "B787", "A350", "B747"]


def seed_countries():
    """Insert countries, return {iso_code: uuid} map."""
    print("  Seeding countries...")
    country_ids = {}
    rows = []
    for name, iso, continent, currency in COUNTRIES:
        uid = str(uuid.uuid4())
        country_ids[iso] = uid
        rows.append((uid, name, iso, continent, currency))

    execute_values(cur, """
        INSERT INTO countries (id, name, iso_code, continent, currency)
        VALUES %s
        ON CONFLICT (iso_code) DO NOTHING
    """, rows)
    print(f"    ✓ {len(rows)} countries")
    return country_ids


def seed_cities(country_ids):
    """Insert cities, return {iata_code: uuid} map."""
    print("  Seeding cities...")
    city_ids = {}
    rows = []
    for name, iata, timezone, country_iso, lat, lon in CITIES:
        uid = str(uuid.uuid4())
        city_ids[iata] = uid
        country_id = country_ids.get(country_iso)
        if not country_id:
            print(f"    WARN: No country for {country_iso}, skipping {name}")
            continue
        rows.append((uid, country_id, name, iata, timezone, lon, lat))

    for row in rows:
        cur.execute("""
            INSERT INTO cities
                (id, country_id, name, iata_city_code, timezone, coordinates)
            VALUES
                (%s, %s, %s, %s, %s, ST_SetSRID(ST_MakePoint(%s, %s), 4326))
            ON CONFLICT DO NOTHING
        """, row)
    print(f"    ✓ {len(rows)} cities")
    return city_ids


def seed_airports(city_ids):
    """Insert airports, return {iata_code: (uuid, city_uuid)} map."""
    print("  Seeding airports...")
    airport_ids = {}
    rows = []
    for name, iata, city_iata, terminals in AIRPORTS:
        uid = str(uuid.uuid4())
        city_id = city_ids.get(city_iata)
        if not city_id:
            print(f"    WARN: No city for {city_iata}, skipping airport {iata}")
            continue
        airport_ids[iata] = (uid, city_id)
        rows.append((uid, city_id, name, iata, terminals))

    execute_values(cur, """
        INSERT INTO airports (id, city_id, name, iata_code, terminal_count)
        VALUES %s
        ON CONFLICT (iata_code) DO NOTHING
    """, rows)
    print(f"    ✓ {len(rows)} airports")
    return airport_ids


def seed_flights(airport_ids, city_ids):
    print("  Seeding flights...")
    flight_rows = []
    graph_rows  = []
    now = datetime.utcnow()

    for origin_iata, dest_iata, airline_code, airline_name, duration_min, base_price in ROUTE_TEMPLATES:
        origin_airport = airport_ids.get(origin_iata)
        dest_airport   = airport_ids.get(dest_iata)
        if not origin_airport or not dest_airport:
            continue

        origin_airport_id, origin_city_id = origin_airport
        dest_airport_id,   dest_city_id   = dest_airport

        # Generate ~20 flights per route over 90 days
        for i in range(20):
            flight_id     = str(uuid.uuid4())
            flight_number = f"{airline_code}{random.randint(100, 999)}"
            # Spread departures across the next 90 days
            days_ahead    = random.randint(1, 90)
            hour          = random.choice([6, 8, 10, 12, 14, 16, 18, 20, 22])
            departure     = now.replace(hour=hour, minute=0, second=0, microsecond=0) \
                            + timedelta(days=days_ahead)
            arrival       = departure + timedelta(minutes=duration_min)
            seats_total   = random.choice([150, 180, 220, 300, 350])
            seats_booked  = random.randint(0, int(seats_total * 0.85))
            # Price varies ±20% from base
            variance      = random.uniform(0.8, 1.2)
            current_price = round(base_price * variance, 2)
            cabin         = random.choices(
                ["ECONOMY", "BUSINESS"], weights=[85, 15]
            )[0]
            aircraft      = random.choice(AIRCRAFT)

            flight_rows.append((
                flight_id, flight_number, airline_code, airline_name,
                origin_airport_id, dest_airport_id,
                departure, arrival,
                seats_total, seats_booked,
                base_price, current_price, cabin, aircraft,
                "SCHEDULED", True
            ))

            graph_rows.append((
                str(uuid.uuid4()), flight_id,
                origin_city_id, dest_city_id,
                current_price, duration_min,
                60,
                departure, arrival,
                airline_code, cabin, True
            ))

    execute_values(cur, """
        INSERT INTO flights (
            id, flight_number, airline_code, airline_name,
            origin_airport_id, dest_airport_id,
            scheduled_departure, scheduled_arrival,
            seats_total, seats_booked,
            base_price, current_price, cabin, aircraft_type,
            status, is_active
        ) VALUES %s
        ON CONFLICT DO NOTHING
    """, flight_rows)

    execute_values(cur, """
        INSERT INTO flight_routes (
            id, flight_id,
            origin_city_id, dest_city_id,
            weight_price, weight_duration_min,
            min_connection_min,
            departure_at, arrival_at,
            airline_code, cabin, is_active
        ) VALUES %s
        ON CONFLICT DO NOTHING
    """, graph_rows)

    print(f"    ✓ {len(flight_rows)} flights, {len(graph_rows)} graph edges")
    return flight_rows


def main():
    print("🌱 Seeding flight-service database...")
    try:
        country_ids = seed_countries()
        city_ids    = seed_cities(country_ids)
        airport_ids = seed_airports(city_ids)
        seed_flights(airport_ids, city_ids)
        conn.commit()
        print("✅ Flight seed complete")
    except Exception as e:
        conn.rollback()
        print(f"❌ Seed failed: {e}")
        raise
    finally:
        cur.close()
        conn.close()


if __name__ == "__main__":
    main()