# Paddle Board Express Backend

## Startup

This is a biff project.

Run `bb dev` to get started. See `bb tasks` for other commands.

## REPL

After startup, connect to the already running REPL at port 7888.

## XTDB Local Storage

If you want to clear your development instance of XTDB's on-disk storage:

1. Stop the application
2. Delete the `storage/xtdb` directory.
3. Start the application

Maybe there is a way to do this without restarting the application.

## Stripe Webhooks

In order to have a functioning environment that reacts to stripe webhooks for payment events, you will need to install the [Stripe CLI](https://stripe.com/docs/stripe-cli) and run `stripe listen --forward-to localhost:8080/api/stripe_webhook`.

It's not a requirement to simply start the app, but to complete the rental flow it is, there are ways of simulating webhooks without running Stripe CLI that can be explored later on.

## Configuration

Configuration is checked into source control and is the `config.edn` file. This config file makes references to secrets that Biff accesses via environment variables. Those environment variables are setup in the `secrets.env` file which is not checked into source control. 

The `secrets.example` is checked into source control, rename it to `secrets.env`.

You will need to find values for: 
- COM_BE_DO_SPACES_ACCESS_KEY_ID
- COM_BE_DO_SPACES_SECRET_ACCESS_KEY
- COM_BE_STRIPE_API_KEY

## API

### Commands

### Queries