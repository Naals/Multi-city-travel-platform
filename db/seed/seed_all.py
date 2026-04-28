"""
seed_all.py
===========
Usage:
    cd db/seed
    pip install -r requirements.txt
    python seed_all.py

    # Seed specific service only:
    python seed_all.py --service flight
    python seed_all.py --service users
"""

import sys
import time
import argparse
import psycopg2
import os
from dotenv import load_dotenv

load_dotenv()


def wait_for_db(host, port, dbname, user, password, retries=10):
    """Wait for a Postgres DB to be ready before seeding."""
    for attempt in range(1, retries + 1):
        try:
            conn = psycopg2.connect(
                host=host, port=port, dbname=dbname,
                user=user, password=password, connect_timeout=3
            )
            conn.close()
            print(f"    ✓ {dbname}@{host}:{port} is ready")
            return True
        except psycopg2.OperationalError:
            print(f"    ⏳ Waiting for {dbname} (attempt {attempt}/{retries})...")
            time.sleep(3)
    raise RuntimeError(f"DB {dbname} not ready after {retries} attempts")


def seed_auth():
    print("\n── Auth Service ──────────────────────────────")
    wait_for_db(
        os.getenv("AUTH_DB_HOST", "localhost"),
        os.getenv("AUTH_DB_PORT", "5432"),
        os.getenv("AUTH_DB_NAME", "auth_db"),
        os.getenv("AUTH_DB_USER", "auth_user"),
        os.getenv("AUTH_DB_PASSWORD", "auth_secret_pass"),
    )
    import seed_auth as s
    s.main()


def seed_users():
    print("\n── User Service ──────────────────────────────")
    wait_for_db(
        os.getenv("USER_DB_HOST", "localhost"),
        os.getenv("USER_DB_PORT", "5433"),
        os.getenv("USER_DB_NAME", "user_db"),
        os.getenv("USER_DB_USER", "user_user"),
        os.getenv("USER_DB_PASSWORD", "user_secret_pass"),
    )
    import seed_users as s
    s.main()


def seed_flights():
    print("\n── Flight Service ────────────────────────────")
    wait_for_db(
        os.getenv("FLIGHT_DB_HOST", "localhost"),
        os.getenv("FLIGHT_DB_PORT", "5434"),
        os.getenv("FLIGHT_DB_NAME", "flight_db"),
        os.getenv("FLIGHT_DB_USER", "flight_user"),
        os.getenv("FLIGHT_DB_PASSWORD", "flight_secret_pass"),
    )
    import seed_flights as s
    s.main()


def seed_bookings():
    """Seed sample bookings — must run AFTER auth + users + flights."""
    print("\n── Booking Service ───────────────────────────")
    wait_for_db(
        os.getenv("BOOKING_DB_HOST", "localhost"),
        os.getenv("BOOKING_DB_PORT", "5435"),
        os.getenv("BOOKING_DB_NAME", "booking_db"),
        os.getenv("BOOKING_DB_USER", "booking_user"),
        os.getenv("BOOKING_DB_PASSWORD", "booking_secret_pass"),
    )
    import seed_bookings as s
    s.main()


def seed_payments():
    """Must run AFTER bookings."""
    print("\n── Payment Service ───────────────────────────")
    wait_for_db(
        os.getenv("PAYMENT_DB_HOST", "localhost"),
        os.getenv("PAYMENT_DB_PORT", "5436"),
        os.getenv("PAYMENT_DB_NAME", "payment_db"),
        os.getenv("PAYMENT_DB_USER", "payment_user"),
        os.getenv("PAYMENT_DB_PASSWORD", "payment_secret_pass"),
    )
    import seed_payments as s
    s.main()


def seed_reviews():
    """Must run AFTER payments (eligibility records needed)."""
    print("\n── Review Service ────────────────────────────")
    wait_for_db(
        os.getenv("REVIEW_DB_HOST", "localhost"),
        os.getenv("REVIEW_DB_PORT", "5437"),
        os.getenv("REVIEW_DB_NAME", "review_db"),
        os.getenv("REVIEW_DB_USER", "review_user"),
        os.getenv("REVIEW_DB_PASSWORD", "review_secret_pass"),
    )
    import seed_reviews as s
    s.main()


# ── Execution order matters — FK dependencies across services ──
SEEDERS = {
    "auth":     seed_auth,
    "users":    seed_users,
    "flights":  seed_flights,
    "bookings": seed_bookings,
    "payments": seed_payments,
    "reviews":  seed_reviews,
}

ALL_IN_ORDER = ["auth", "users", "flights", "bookings", "payments", "reviews"]


def main():
    parser = argparse.ArgumentParser(description="Multi-City Travel — DB Seed Runner")
    parser.add_argument(
        "--service",
        choices=list(SEEDERS.keys()),
        help="Seed a specific service only"
    )
    args = parser.parse_args()

    start = time.time()
    print("🌱 Multi-City Travel — Database Seeder")
    print("=" * 50)

    if args.service:
        SEEDERS[args.service]()
    else:
        for name in ALL_IN_ORDER:
            SEEDERS[name]()

    elapsed = round(time.time() - start, 2)
    print(f"\n{'=' * 50}")
    print(f"✅ All seeds complete in {elapsed}s")


if __name__ == "__main__":
    main()